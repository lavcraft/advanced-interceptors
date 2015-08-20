package org.cxf.interceptor.logging;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author tolkv
 * @since 15/08/15
 */
@Slf4j
public class CustomizableCxfInterceptor extends AbstractPhaseInterceptor {
  private final Consumer<LoggingContext> loggingAction;

  public CustomizableCxfInterceptor(Consumer<LoggingContext> loggingAction) {
    this(Phase.SETUP, loggingAction);
  }

  public CustomizableCxfInterceptor(String phase, Consumer<LoggingContext> loggingAction) {
    super(phase);
    this.loggingAction = loggingAction;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleMessage(Message message) throws Fault {
    try {
      Logger messageLogger = getMessageLogger(message);

      if (message.containsKey(LoggingMessage.ID_KEY)) {
        return;
      }

      final String id = getMessageId(message);

      message.put(LoggingMessage.ID_KEY, id);
      Optional.ofNullable(message.getContent(List.class))
          .ifPresent(list -> list.forEach(o -> loggingAction.accept(
                  new LoggingContext(id, o, message, messageLogger)
              )
          ));

    } catch (UndeclaredThrowableException exception) {
      Throwable rootCause = Throwables.getRootCause(exception);
      Throwables.propagateIfInstanceOf(rootCause, UncheckedException.class);

      logUnexpectedException(Throwables.getRootCause(exception));
    } catch (Exception e) {
      Throwables.propagateIfInstanceOf(e, UncheckedException.class);
      Throwable rootCause = Throwables.getRootCause(e);
      logUnexpectedException(rootCause);
    }
  }

  private String getMessageId(Message message) {
    String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
    if (id == null) {
      id = LoggingMessage.nextId();
      message.getExchange().put(LoggingMessage.ID_KEY, id);
    }
    return id;
  }

  void logUnexpectedException(Throwable ex) {
    log.debug("Unexpected error: class: {}, message: {}", ex.getClass(), ex.getMessage());
  }

  protected Logger getMessageLogger(Message msg) {
    Endpoint ep = msg.getExchange().getEndpoint();
    if (ep == null || ep.getEndpointInfo() == null) {
      return getLogger();
    }
    EndpointInfo endpoint = ep.getEndpointInfo();
    if (endpoint.getService() == null) {
      return getLogger();
    }

    Logger logger = endpoint.getProperty("MessageLogger", Logger.class);
    if (logger == null) {
      String serviceName = endpoint.getService().getName().getLocalPart();
      InterfaceInfo iface = endpoint.getService().getInterface();
      String portName = endpoint.getName().getLocalPart();
      String portTypeName = iface.getName().getLocalPart();
      String logName = "org.apache.cxf.services." + serviceName + "."
          + portName + "." + portTypeName;
      logger = LoggerFactory.getLogger(logName);
      endpoint.setProperty("MessageLogger", logger);
    }
    return logger;
  }

  protected Logger getLogger() {
    return log;
  }
}

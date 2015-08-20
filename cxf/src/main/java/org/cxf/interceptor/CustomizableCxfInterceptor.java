package org.cxf.interceptor;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

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
  private final Consumer<Object> loggingAction;

  public CustomizableCxfInterceptor(Consumer<Object> loggingAction) {
    this(Phase.SETUP, loggingAction);
  }

  public CustomizableCxfInterceptor(String phase, Consumer<Object> loggingAction) {
    super(phase);
    this.loggingAction = loggingAction;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleMessage(Message message) throws Fault {
    try {
      Optional.ofNullable(message.getContent(List.class))
          .ifPresent(list -> list.forEach(loggingAction::accept));
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

  void logUnexpectedException(Throwable ex) {
    log.debug("Unexpected error: class: {}, message: {}", ex.getClass(), ex.getMessage());
  }
}

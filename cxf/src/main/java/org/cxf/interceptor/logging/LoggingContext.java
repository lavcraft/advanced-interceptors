package org.cxf.interceptor.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;

/**
 * @author tolkv
 * @since 20/08/15
 */
@Data
@Builder
@AllArgsConstructor
public class LoggingContext {
  private String id;
  private Object targetObject;
  private Message message;
  private Logger logger;
}

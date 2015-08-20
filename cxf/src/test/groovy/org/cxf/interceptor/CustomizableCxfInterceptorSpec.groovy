package org.cxf.interceptor

import org.apache.cxf.common.i18n.UncheckedException
import org.apache.cxf.message.Message
import org.cxf.interceptor.test.NestedObject
import org.cxf.interceptor.test.RequestWithDNestedObject
import spock.lang.Specification

import java.util.logging.Logger

/**
 * @author tolkv
 * @since 20/08/15
 */
class CustomizableCxfInterceptorSpec extends Specification {
  @SuppressWarnings("GroovyAssignabilityCheck")
  def 'should throw suppress generic exceptions'() {
    given:
    def targetObject = RequestWithDNestedObject.builder()
        .rootProperty('rootPropertyValue')
        .nested(NestedObject.builder()
        .property('test')
        .build())
        .build()

    def msgMock = Mock(Message) {
      getContent(_) >> [targetObject]
    }

    def interceptorSpy = Spy(CustomizableCxfInterceptor, constructorArgs: ['setup', consumer])

    when:
    interceptorSpy.handleMessage(msgMock)

    then:
    1 * msgMock.getContent(_) >> [targetObject]
    1 * interceptorSpy.logUnexpectedException(_)

    where:
    consumer << [{ it -> throw new Exception("Exception") },
                 { it -> throw new RuntimeException("Exception") }]
  }

  def 'should throw Fault exception'() {
    given:
    def targetObject = RequestWithDNestedObject.builder()
        .rootProperty('rootPropertyValue')
        .nested(NestedObject.builder()
        .property('test')
        .build())
        .build()

    def msgMock = Mock(Message) {
      getContent(_) >> [targetObject]
    }

    def interceptorSpy = Spy(CustomizableCxfInterceptor, constructorArgs:
        ['setup',
         { it -> throw new UncheckedException(new org.apache.cxf.common.i18n.Message("", Logger.getLogger("TEST")))}
        ]
    )

    when:
    interceptorSpy.handleMessage(msgMock)

    then:
    1 * msgMock.getContent(_) >> [targetObject]
    thrown(UncheckedException)
  }
}

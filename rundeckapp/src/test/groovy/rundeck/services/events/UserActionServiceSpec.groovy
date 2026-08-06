package rundeck.services.events

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.WriterAppender
import org.apache.logging.log4j.core.layout.PatternLayout
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import rundeck.services.UserService
import spock.lang.Specification

class UserActionServiceSpec extends Specification implements ServiceUnitTest<UserActionService>, DataTest {


    void "handleAuthenticationSuccessEvent"() {
        setup:
        service.userService = Mock(UserService){
            registerLogin(_) >> _
        }
        AuthenticationSuccessEvent event = Mock(AuthenticationSuccessEvent){
            getAuthentication() >> Mock(Authentication){
                getName() >> "testUser"
            }
        }
        when:
        service.handleAuthenticationSuccessEvent(event)
        then:
        true
    }

    void "handleAuthenticationFailureServiceExceptionEvent logs warning for JAAS login failure"() {
        setup:
        service.userService = Mock(UserService)
        AuthenticationFailureServiceExceptionEvent event = Mock(AuthenticationFailureServiceExceptionEvent) {
            getAuthentication() >> Mock(Authentication) { getName() >> "baduser" }
        }
        def logOutput = new StringWriter()
        def ctx = (LoggerContext) LogManager.getContext(false)
        def config = ctx.getConfiguration()
        def appender = WriterAppender.newBuilder()
            .setConfiguration(config)
            .setName("UserActionServiceTestCapture")
            .setTarget(logOutput)
            .setLayout(PatternLayout.newBuilder().withPattern("[%level] %msg%n").withConfiguration(config).build())
            .build()
        appender.start()
        config.getRootLogger().addAppender(appender, Level.WARN, null)
        ctx.updateLoggers()

        when:
        service.handleAuthenticationFailureServiceExceptionEvent(event)

        then:
        logOutput.toString().contains("baduser")

        cleanup:
        config.getRootLogger().removeAppender("UserActionServiceTestCapture")
        appender.stop()
        ctx.updateLoggers()
    }

    void "handleAuthenticationFailureServiceExceptionEvent handles null authentication gracefully"() {
        setup:
        service.userService = Mock(UserService)
        AuthenticationFailureServiceExceptionEvent event = Mock(AuthenticationFailureServiceExceptionEvent) {
            getAuthentication() >> null
        }

        when:
        service.handleAuthenticationFailureServiceExceptionEvent(event)

        then:
        noExceptionThrown()
    }

    void "logout"() {
        setup:
        service.userService = Mock(UserService){
            registerLogout(_) >> _
        }
        Authentication auth = Mock(Authentication){
            getName() >> "testUser"
        }

        when:
        service.logout(null,null, auth)
        then:
        true
    }
}

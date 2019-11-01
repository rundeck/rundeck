package rundeck.services.events

import grails.test.mixin.TestFor
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import rundeck.services.UserService
import spock.lang.Specification

@TestFor(UserActionService)
class UserActionServiceSpec extends Specification {


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

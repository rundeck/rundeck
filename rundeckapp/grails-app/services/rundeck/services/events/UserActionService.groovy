package rundeck.services.events

import grails.gorm.transactions.Transactional
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import rundeck.services.FrameworkService
import rundeck.services.UserService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Transactional
class UserActionService implements LogoutHandler{

    UserService userService
    FrameworkService frameworkService

    @EventListener
    void handleAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        if(extractUsername(event?.authentication) != null){
            String sessionId = event.getSource()?.details?.sessionId
            userService.registerLogin(extractUsername(event.authentication), sessionId)
        }else{
            log.error("Null user name on handleAuthenticationSuccessEvent")
        }
    }

    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if(extractUsername(authentication) != null){
            userService.registerLogout(extractUsername(authentication))
        }else{
            log.error("Null user name on logout event")
        }
    }

    private String extractUsername(Authentication authentication) {
        if (!authentication){
            log.error("Null authentication on event")
            return null
        }
        return authentication.name ?: authentication.principal.name ?: null
    }
}

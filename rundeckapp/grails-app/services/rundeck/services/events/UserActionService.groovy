package rundeck.services.events

import grails.gorm.transactions.Transactional
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationFailureServiceExceptionEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.logout.LogoutHandler
import rundeck.services.FrameworkService
import rundeck.services.UserService

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Transactional
class UserActionService implements LogoutHandler{

    UserService userService

    @EventListener
    void handleAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        if(extractUsername(event?.authentication) != null){
            String sessionId = null
            if(userService.isSessionIdRegisterEnabled()){
                sessionId = event.getSource()?.details?.sessionId
            }
            userService.registerLogin(extractUsername(event.authentication), sessionId)
        }else{
            log.error("Null user name on handleAuthenticationSuccessEvent")
        }
    }

    /**
     * Handles failed authentication events produced by JAAS login providers.
     * When {@code rundeck.jaaslogin=true}, Spring Security publishes
     * {@code AuthenticationFailureServiceExceptionEvent} for failed logins instead of
     * {@code AuthenticationFailureBadCredentialsEvent}, leaving a gap in login-failure tracking.
     *
     * @param event the JAAS authentication failure event
     */
    @EventListener
    void handleAuthenticationFailureServiceExceptionEvent(AuthenticationFailureServiceExceptionEvent event) {
        def username = extractUsername(event?.authentication)
        if (username != null) {
            log.warn("Failed login attempt (JAAS) for user: ${username}")
        } else {
            log.error("Null user name on handleAuthenticationFailureServiceExceptionEvent")
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

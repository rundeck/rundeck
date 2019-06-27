package rundeck.services.audit

import com.dtolabs.rundeck.core.audit.AuditEvent
import com.dtolabs.rundeck.core.audit.AuditEvent.AuditEventType
import com.dtolabs.rundeck.plugins.audit.AuditEventsHandler
import grails.gorm.transactions.Transactional
import org.apache.log4j.Logger
import org.springframework.context.event.EventListener
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.LogoutHandler
import rundeck.services.FrameworkService

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.stream.Collectors

@Transactional
class AuditEventsService
        implements LogoutHandler {

    static final Logger LOG = Logger.getLogger(AuditEventsService.class)

    final FrameworkService frameworkService
    final AsyncTaskExecutor asyncTaskExecutor

    AuditEventsService(FrameworkService frameworkService) {
        this.frameworkService = frameworkService

        asyncTaskExecutor = new ThreadPoolTaskExecutor()
        asyncTaskExecutor.setCorePoolSize(1)
        asyncTaskExecutor.setMaxPoolSize(1)
        asyncTaskExecutor.initialize()
    }


    /**
     * Handles and authentication success event
     * @param event
     */
    @EventListener
    void handleAuthenticationSuccessEvent(AuthenticationSuccessEvent event) {
        buildEvent(AuditEventType.LOGIN_SUCCESS)
                .setUsername(extractUsername(event.authentication))
                .setRoles(extractAuthorities(event.authentication))
                .publish()
    }

    @EventListener
    void handleAuthenticationFailureEvent(AuthenticationFailureBadCredentialsEvent event) {
        if (!event.authentication) {
            LOG.error("Null authentication on login failure event. Cancelling event dispatch.")
            return
        }

        buildEvent(AuditEventType.LOGIN_FAILED)
                .setUsername(extractUsername(event.authentication))
                .setRoles(extractAuthorities(event.authentication))
                .publish()

    }


    /**
     * Handles a logout event.
     *
     */
    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (!authentication) {
            LOG.error("Null authentication on logout event. Cancelling event dispatch.")
            return
        }

        buildEvent(AuditEventType.LOGOUT)
                .setUsername(extractUsername(authentication))
                .setRoles(extractAuthorities(authentication))
                .publish()
    }


    /**
     * Creates a new event builder of the specified type.
     * After the builder is set, call {@link AuditEventBuilder#publish()} to publish the event.
     *
     * @param eventType
     * @return
     */
    AuditEventBuilder buildEvent(AuditEventType eventType) {
        return new AuditEventBuilder(eventType);
    }


    /**
     * Dispatch the event.
     */
    private dispatchEvent(AuditEventBuilder eventBuilder) {

        // If no user specified, set the user from the current context.
        if (!eventBuilder.username) {
            Authentication auth = SecurityContextHolder.context.authentication
            eventBuilder.setUsername(extractUsername(auth))
            eventBuilder.setRoles(extractAuthorities(auth))
        }

        AuditEvent event = eventBuilder.build();
        System.out.println("DISPATCHNG EVENT!!!!!!!   " + event)

        // Dispatch to plugins.
        LOG.debug("Dispatching event to plugin handlers.")

        asyncTaskExecutor.execute {
            frameworkService.pluginService.listPlugins(AuditEventsHandler.class)
                    .values().stream()
                    .peek {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Dispatching event to handler {" + it.name + "}: " + event)
                        }
                    }
                    .map { it.instance }
                    .forEach { handler ->
                        try {
                            dispatchToHandler(event, handler)
                        }
                        catch (Exception e) {
                            LOG.error("Error dispatching event to handler plugin: " + e.getMessage(), e)
                        }
                    }
        }
    }


    private static dispatchToHandler(AuditEvent event, AuditEventsHandler handler) {

        // Call specific callbacks
        switch (event.eventType) {
            case AuditEventType.LOGIN_SUCCESS:
                handler.onLoginSuccessfulEvent(event)
                break

            case AuditEventType.LOGIN_FAILED:
                handler.onLoginFailureEvent(event)
                break

            case AuditEventType.LOGOUT:
                handler.onLogoutSuccessfulEvent(event)
                break

            case AuditEventType.PROJECT_ACCESS:
                handler.onProjectAccessEvent(event)
                break

        }

        // Call general callback
        handler.onEvent(event)
    }


    /**
     * Extract the username from an authentication object.
     */
    private static String extractUsername(Authentication authentication) {
        if (!authentication) {
            return null
        }
        return authentication.name ?: authentication.principal.name ?: null
    }


    /**
     * Extract authorities from an authentication object as a string list.
     */
    private static List<String> extractAuthorities(Authentication authentication) {
        if (!authentication || !authentication.authorities) {
            return new ArrayList<String>()
        }
        return authentication.authorities.stream()
                .map { it.authority }
                .collect(Collectors.toList())
    }


    /**
     * Builds a new Audit Event
     */
    public class AuditEventBuilder {

        private Date timestamp = new Date()
        private String username = null
        private Collection<String> roles = []
        private String projectName = null
        private AuditEventType eventType


        private AuditEventBuilder(AuditEventType eventType) {
            this.eventType = eventType
        }

        /**
         * Triggers this event publishing.
         */
        public void publish() {
            dispatchEvent(this)
        }

        /**
         * Sets the event timestamp. Default is the current time.
         * @param timestamp
         * @return
         */
        AuditEventBuilder setTimestamp(Date timestamp) {
            this.timestamp = timestamp
            return this
        }

        /**
         * Sets the username associated with this event.
         * If not set, the current context user will be used.
         * @param username
         * @return
         */
        AuditEventBuilder setUsername(String username) {
            this.username = username
            return this
        }

        /**
         * Sets the roles of the user.
         * @param roles
         * @return
         */
        AuditEventBuilder setRoles(List<String> roles) {
            this.roles = roles
            return this
        }

        /**
         * Sets the project name associated with this event.
         * @param projectName
         * @return
         */
        AuditEventBuilder setProjectName(String projectName) {
            this.projectName = projectName
            return this
        }

        /**
         * Changes the event type.
         * @param eventType
         * @return
         */
        AuditEventBuilder setEventType(AuditEventType eventType) {
            this.eventType = eventType
            return this
        }

        /**
         * Builds the event adapter as a read-only object.
         * @return
         */
        private AuditEvent build() {

            final etype = eventType
            final ts = timestamp
            final user = username
            final userroles = Collections.unmodifiableList(roles)
            final projName = projectName

            return new AuditEvent() {
                @Override
                AuditEventType getEventType() {
                    return etype
                }

                @Override
                Date getTimestamp() {
                    return ts
                }

                @Override
                String getUsername() {
                    return user
                }

                @Override
                List<String> getUserRoles() {
                    return userroles
                }

                @Override
                String getProjectName() {
                    return projName
                }


                @Override
                String toString() {
                    return "AuditEvent{" +
                            "timestamp=" + getTimestamp() +
                            ", username='" + getUsername() + '\'' +
                            ", roles=" + getUserRoles() +
                            ", projectName='" + getProjectName() + '\'' +
                            ", eventType=" + getEventType() +
                            '}'
                }
            }
        }
    }
}

package rundeck.services.audit

import com.dtolabs.rundeck.core.audit.AuditEvent
import com.dtolabs.rundeck.plugins.audit.AuditEventListener
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.stream.Collectors

/**
 *
 * Service for capturing and firing auditing events like login, logout or resource access
 *
 * @author Alberto Hormazabal
 *
 */
@Transactional
class AuditEventsService
        implements LogoutHandler {

    static final Logger LOG = Logger.getLogger(AuditEventsService.class)

    final FrameworkService frameworkService
    final AsyncTaskExecutor asyncTaskExecutor
    final CopyOnWriteArrayList<AuditEventListener> internalListeners = new CopyOnWriteArrayList<>()

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
        eventBuilder()
                .setUsername(extractUsername(event.authentication))
                .setUserRoles(extractAuthorities(event.authentication))
                .setAction(AuditEvent.Action.login_success)
                .setResourceType(AuditEvent.ResourceType.user)
                .setResourceName(extractUsername(event.authentication))
                .publish()
    }

    @EventListener
    void handleAuthenticationFailureEvent(AuthenticationFailureBadCredentialsEvent event) {
        if (!event.authentication) {
            LOG.error("Null authentication on login failure event. Cancelling event dispatch.")
            return
        }

        eventBuilder()
                .setUsername(extractUsername(event.authentication))
                .setUserRoles(extractAuthorities(event.authentication))
                .setAction(AuditEvent.Action.login_failed)
                .setResourceType(AuditEvent.ResourceType.user)
                .setResourceName(extractUsername(event.authentication))
                .publish()
    }


    /**
     * Captures logout events.
     *
     */
    @Override
    void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (!authentication) {
            LOG.error("Null authentication on logout event. Cancelling event dispatch.")
            return
        }

        eventBuilder()
                .setUsername(extractUsername(authentication))
                .setUserRoles(extractAuthorities(authentication))
                .setAction(AuditEvent.Action.logout)
                .setResourceType(AuditEvent.ResourceType.user)
                .setResourceName(extractUsername(authentication))
                .publish()
    }


    /**
     * Creates a new event builder.
     * After the builder is configured, call {@link AuditEventBuilder#publish()} to publish the event.
     * The builder can be published several times.
     *
     * @param eventType
     * @return
     */
    AuditEventBuilder eventBuilder() {
        return new AuditEventBuilder()
    }


    /**
     * Add a new listener to the internal register.
     * @param listener
     */
    void addListener(AuditEventListener listener) {
        internalListeners.add(listener)
    }

    /**
     * Removes the listener from the internal listener register.
     * @param listener
     */
    void removeListener(AuditEventListener listener) {
        internalListeners.remove(listener)
    }


    /**
     * Dispatch the event.
     */
    private dispatchEvent(AuditEventBuilder eventBuilder) {

        // If no user specified, set the user from the current context.
        if (!eventBuilder.username) {
            Authentication auth = SecurityContextHolder.context.authentication
            eventBuilder.setUsername(extractUsername(auth))
            eventBuilder.setUserRoles(extractAuthorities(auth))
        }

        AuditEvent event = eventBuilder.build()

        if (LOG.isDebugEnabled())
            LOG.debug("Dispatching audit event: " + event)

        asyncTaskExecutor.execute {

            // dispatch internal listeners.
            internalListeners.each { listener ->
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Dispatching event to internal listener {" + listener + "}: " + event)
                }
                dispatchToListener(event, listener)
            }

            // dispatch to plugins
            frameworkService.pluginService.listPlugins(AuditEventListener.class)
                    .values().stream()
                    .peek {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Dispatching event to plugin handler {" + it.name + "}: " + event)
                        }
                    }
                    .map { it.instance }
                    .forEach { dispatchToListener(event, it) }
        }
    }


    /**
     * Dispatch an event to a listener.
     */
    private static dispatchToListener(AuditEvent event, AuditEventListener listener) {

        try {

            // Call general callback
            listener.onEvent(event)

            // Call specific callbacks
            if (AuditEvent.ResourceType.user.equals(event.resourceType)) {
                switch (event.action) {
                    case AuditEvent.Action.login_success:
                        listener.onLoginSuccess(event)
                        break

                    case AuditEvent.Action.login_failed:
                        listener.onLoginFailed(event)
                        break

                    case AuditEvent.Action.logout:
                        listener.onLogout(event)
                        break
                }
            } else if (AuditEvent.ResourceType.project.equals(event.resourceType)) {
                switch (event.action) {
                    case AuditEvent.Action.view:
                        listener.onProjectView(event)
                        break
                }
            }

        }
        catch (Exception e) {
            LOG.error("Error dispatching event to handler plugin: " + e.getMessage(), e)
        }
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
    class AuditEventBuilder {

        private String username = null
        private Collection<String> userRoles = []
        private AuditEvent.Action action = null
        private AuditEvent.ResourceType resourceType = null
        private String resourceName = null


        private AuditEventBuilder() {
        }

        /**
         * Triggers this event publishing.
         * A new event will be generated and published with an immutable copy of the data in this builder.
         * Multiple calls to this method will cause multiple events to be published.
         * <p>
         *     If the username is not set, the user and roles will be set
         *     from the current security context.
         */
        public void publish() {
            dispatchEvent(this)
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
        AuditEventBuilder setUserRoles(List<String> roles) {
            this.userRoles = roles
            return this
        }

        /**
         * Sets the event action.
         * @param eventType
         * @return
         */
        AuditEventBuilder setAction(AuditEvent.Action action) {
            this.action = action
            return this
        }

        /**
         * Sets the project name associated with this event.
         * @param projectName
         * @return
         */
        AuditEventBuilder setResourceType(AuditEvent.ResourceType resourceType) {
            this.resourceType = resourceType
            return this
        }

        /**
         * Sets the project name associated with this event.
         * @param projectName
         * @return
         */
        AuditEventBuilder setResourceName(String name) {
            this.resourceName = name
            return this
        }

        /**
         * Builds a new immutable event object
         * @return
         */
        private AuditEvent build() {

            return new AuditEvent() {
                // Copy the data.
                final ts = new Date()
                final user = AuditEventBuilder.this.username
                final roles = Collections.unmodifiableList(new ArrayList(AuditEventBuilder.this.userRoles))
                final action = AuditEventBuilder.this.action
                final rtype = AuditEventBuilder.this.resourceType
                final rname = AuditEventBuilder.this.resourceName

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
                    return roles
                }

                @Override
                AuditEvent.Action getAction() {
                    return action
                }

                @Override
                AuditEvent.ResourceType getResourceType() {
                    return rtype
                }

                @Override
                String getResourceName() {
                    return rname
                }


                @Override
                String toString() {
                    return "AuditEvent {" +
                            "timestamp=" + ts +
                            ", username=" + user +
                            ", userRoles=" + roles +
                            ", action=" + action +
                            ", resourceType=" + rtype +
                            ", resourceName=" + rname +
                            '}'
                }
            }
        }

    }
}

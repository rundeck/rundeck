package rundeck.services.audit

import com.dtolabs.rundeck.core.audit.*
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.audit.AuditEventListener
import com.dtolabs.rundeck.plugins.audit.AuditEventListenerPlugin
import grails.events.annotation.Subscriber
import grails.util.Holders
import groovy.transform.PackageScope
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import org.rundeck.app.acl.ACLFileManagerListener
import org.rundeck.app.acl.AppACLContext
import org.rundeck.app.acl.ContextACLManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
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
import java.util.function.Function
import java.util.stream.Collectors

/**
 *
 * Service for capturing and firing auditing events like login, logout or resource access
 *
 * @author Alberto Hormazabal
 *
 */
class AuditEventsService
    implements LogoutHandler {

    static final Logger LOG = LoggerFactory.getLogger(AuditEventsService.class)

    FrameworkService frameworkService

    private ContextACLManager<AppACLContext> aclFileManagerService
    protected AsyncTaskExecutor asyncTaskExecutor
    protected final CopyOnWriteArrayList<AuditEventListener> internalListeners = new CopyOnWriteArrayList<>()

    protected volatile Map<String, DescribedPlugin> installedPlugins = null

    AuditEventsService() {
        LOG.info("Init auditing events service")
        asyncTaskExecutor = new ThreadPoolTaskExecutor()
        asyncTaskExecutor.setCorePoolSize(1)
        asyncTaskExecutor.setMaxPoolSize(1)
        asyncTaskExecutor.initialize()
    }

    @Subscriber('rundeck.bootstrap')
    void init() throws Exception {
        Holders.getApplicationContext()
            .getBean("aclFileManagerService", ContextACLManager)
            .addListenerMap(buildACLFileListeners())
    }

    /**
     * Returns the cache of listener plugins.
     * Cache is implemented lazily, so if the cache is not initialized, the plugins
     * will be instanced and configured, and the cache will be built.
     * @return
     */
    private Map<String, DescribedPlugin<AuditEventListenerPlugin>> getListenerPlugins() {
        if (installedPlugins == null) {
            synchronized (this) {
                if (installedPlugins == null) {
                    installedPlugins = frameworkService.pluginService.listPluginDescriptions(ServiceNameConstants.AuditEventListener)
                            .collectEntries {
                                [(it.name): initializePluginInstance(it)]
                            }
                }
            }
        }
        installedPlugins
    }

    /**
     * Initializes a plugin instance.
     * @param pluginDescription
     * @return
     */
    private DescribedPlugin<AuditEventListenerPlugin> initializePluginInstance(Description pluginDescription) {
        LOG.info("Initializing audit plugin instance: " + pluginDescription.name)
        // Get instance from plugin manager.
        ConfiguredPlugin<AuditEventListenerPlugin> plugin = frameworkService.pluginService.configurePlugin(
                pluginDescription.name,
                null,
                null,
                frameworkService.rundeckFramework as Framework,
                AuditEventListenerPlugin.class)

        // Initialize plugin
        plugin.instance.init()

        return new DescribedPlugin<AuditEventListenerPlugin>(plugin.instance, pluginDescription, pluginDescription.name,null,null)
    }

    /**
     * Builds listeners to map ACL update events to audit events
     * @return
     */
    private Function<AppACLContext, ACLFileManagerListener> buildACLFileListeners() {
        return new Function<AppACLContext, ACLFileManagerListener>() {
            @Override
            ACLFileManagerListener apply(final AppACLContext appACLContext) {
                
                final AuditEventBuilder listenerAuditEventBuilder = eventBuilder()
                    .setResourceType(appACLContext.isSystem() ? ResourceTypes.SYSTEM_ACL : ResourceTypes.PROJECT_ACL)
                final String resNamePrefix = appACLContext.isSystem() ? "[SYSTEM] " :
                    "[Project:${appACLContext.getProject()}] "
                    
                return new ACLFileManagerListener() {
                    @Override
                    void aclFileUpdated(String path) {
                        listenerAuditEventBuilder
                            .setActionType(ActionTypes.UPDATE)
                            .setResourceName(resNamePrefix + path)
                            .publish()
                    }

                    @Override
                    void aclFileDeleted(String path) {
                        listenerAuditEventBuilder
                            .setActionType(ActionTypes.DELETE)
                            .setResourceName(resNamePrefix + path)
                            .publish()
                    }
                }
            }
        }
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
                .setActionType(ActionTypes.LOGIN_SUCCESS)
                .setResourceType(ResourceTypes.USER)
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
                .setActionType(ActionTypes.LOGIN_FAILED)
                .setResourceType(ResourceTypes.USER)
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
                .setActionType(ActionTypes.LOGOUT)
                .setResourceType(ResourceTypes.USER)
                .setResourceName(extractUsername(authentication))
                .publish()
    }

    /**
     * Indicates if the system is enabled and have listeners and/or plugins ready for dispatching.
     * @return
     */
    boolean enabled() {
        return !internalListeners.isEmpty() || !getListenerPlugins().isEmpty()
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

        // Add request data if not defined.
        if (!eventBuilder.httpRequest) {
            try {
                eventBuilder.setHttpRequest(WebUtils.retrieveGrailsWebRequest());
            }
            catch (IllegalStateException e) {
                LOG.debug("Dispatching event outside of web request context: " + e.getMessage(), e)
            }
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
                AuditEventDispatcher.dispatchToListener(event, listener)
            }

            // dispatch to plugins
            this.listenerPlugins
                    .values().stream()
                    .peek {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Dispatching event to plugin handler {" + it.name + "}: " + event)
                        }
                    }
                    .map { it.instance }
                    .forEach { AuditEventDispatcher.dispatchToListener(event, it) }
        }
    }




    /**
     * Extract the username from an authentication object.
     */
    @PackageScope
    static String extractUsername(Authentication authentication) {
        if (!authentication) {
            return null
        }
        if(authentication.name) return authentication.name
        if(authentication.principal instanceof String) return authentication.principal
        return authentication.principal?.name ?: null
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
     * Helper to build new Audit Events.
     *      <p>
     *     To create a builder, use the {@link AuditEventsService#eventBuilder()} method,
     *     ant then complete the data using the returned builder.
     *     </p>
     *     <p> Once the data is set, call the {@link AuditEventBuilder#publish()} method to trigger
     *     the event publishing.</p>
     *     <p>
     *         When the event is published, the data is copied and an immutable event object is created,
     *         this allows developers to set-up a "base" builder, and then modify it and publish many times,
     *         without compromising the data on past events.
     *     </p>
     *
     *
     */
    final class AuditEventBuilder {

        private String username = null
        private Collection<String> userRoles = []
        private String action = null
        private String resourceType = null
        private String resourceName = null
        private GrailsWebRequest httpRequest = null;

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
        void publish() {
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
         * See {@link ActionTypes}
         * @param eventType
         * @return
         */
        AuditEventBuilder setActionType(String actionType) {
            this.action = actionType
            return this
        }

        /**
         * Sets the resource type associated with this event.
         * @param projectName
         * @return
         */
        AuditEventBuilder setResourceType(String resourceType) {
            this.resourceType = resourceType
            return this
        }

        /**
         * Sets the resource name or ID associated with this event.
         * @param projectName
         * @return
         */
        AuditEventBuilder setResourceName(String name) {
            this.resourceName = name
            return this
        }


        /**
         * Sets the request associated with this event.
         * @param httpRequest HTTP Request associated with this event.
         * @return
         */
        AuditEventBuilder setHttpRequest(GrailsWebRequest httpRequest) {
            this.httpRequest = httpRequest
            return this
        }


        /**
         * Builds a new immutable event object
         * @return
         */
        protected AuditEvent build() {
            // Build data copy
            final ts = new Date()
            final user = AuditEventBuilder.this.username
            final roles = Collections.unmodifiableList(new ArrayList(AuditEventBuilder.this.userRoles))
            final action = AuditEventBuilder.this.action
            final rtype = AuditEventBuilder.this.resourceType
            final rname = AuditEventBuilder.this.resourceName
            final serverHostname = frameworkService.getServerHostname()
            final serverUUID = frameworkService.getServerUUID()

            // We cannot reference the request from the event object impl directly because the request api doesn't work well on async scenarios.
            // So we must extract any wanted value here while we are inside the request context (if any)
            def request = AuditEventBuilder.this.httpRequest;
            final sessionID = request?.getSessionId();
            final userAgent = request?.getHeader("User-Agent");

            // build user info impl
            final userInfo = new UserInfo() {
                @Override
                String getUsername() {
                    return user;
                }

                @Override
                List<String> getUserRoles() {
                    return roles;
                }

                @Override
                String toString() {
                    return "{username='" + getUsername() + '\'' +
                            ", userRoles=" + getUserRoles() +
                            '}';
                }
            }

            // build request info impl
            final requestInfo = new RequestInfo() {
                @Override
                String getServerHostname() {
                    return serverHostname
                }

                @Override
                String getServerUUID() {
                    return serverUUID
                }

                @Override
                String getSessionID() {
                    return sessionID
                }

                @Override
                String getUserAgent() {
                    return userAgent
                }

                @Override
                String toString() {
                    return "{serverHostname='" + getServerHostname() + '\'' +
                            ", serverUUID='" + getServerUUID() + '\'' +
                            ", sessionID='" + getSessionID() + '\'' +
                            ", userAgent='" + getUserAgent() + '\'' +
                            '}';
                }
            }

            // build resource info impl
            final resourceInfo = new ResourceInfo() {
                @Override
                String getType() {
                    return rtype
                }

                @Override
                String getName() {
                    return rname
                }

                @Override
                String toString() {
                    return "{resourceType='" + getType() + "'" +
                            ", resourceName='" + getName() + "'" +
                            '}';
                }
            }

            return new AuditEvent() {

                @Override
                Date getTimestamp() {
                    return ts;
                }

                @Override
                String getActionType() {
                    return action
                }

                @Override
                UserInfo getUserInfo() {
                    return userInfo
                }

                @Override
                RequestInfo getRequestInfo() {
                    return requestInfo
                }

                @Override
                ResourceInfo getResourceInfo() {
                    return resourceInfo
                }

                @Override
                String toString() {
                    return "AuditEvent {" +
                            "Timestamp=" + getTimestamp() +
                            ", ActionType='" + getActionType() + '\'' +
                            ", UserInfo=" + getUserInfo() +
                            ", RequestInfo=" + getRequestInfo() +
                            ", ResourceInfo=" + getResourceInfo() +
                            '}';
                }
            }
        }
    }
}

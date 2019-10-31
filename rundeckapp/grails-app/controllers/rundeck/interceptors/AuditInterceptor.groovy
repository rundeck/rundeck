/*
 * Copyright 2017 Rundeck Inc, <http://rundeck.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.interceptors

import com.dtolabs.rundeck.core.audit.ActionTypes
import com.dtolabs.rundeck.core.audit.ResourceTypes
import com.dtolabs.rundeck.core.common.FrameworkResource
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ConfigurationService

import java.util.concurrent.ConcurrentHashMap

/**
 * This filter tracks user access to trigger auditing events.
 * For example, track the user access to different projects.
 *
 * This filter assumes {@link ProjectSelectInterceptor} has already processed the request.
 *
 * @author Alberto Hormazabal
 */
class AuditInterceptor {

    int order = HIGHEST_PRECEDENCE + 101

    /** Default minimum period to wait between project access notifications, in seconds */
    private static final int DEFAULT_PROJECT_MIN_NOTIFICATION_PERIOD = 60 * 30 // default 30 min.

    /** Default minimum time to keep project access track data on cache, in seconds */
    private static final int DEFAULT_CACHE_RETENTION_PERIOD = 60 * 30 // default 30 min.

    def frameworkService
    def auditEventsService
    final ConfigurationService configurationService

    private Long minProjectNotifyPeriodMillis
    private Long cacheRetentionPeriodMillis

    private final ConcurrentHashMap<String, ProjectTrackingEntry> projectAccessTrackingMap
    private transient volatile long lastCachePrune

    @Autowired
    AuditInterceptor(ConfigurationService configurationService) {
        log.debug("Audit Interceptor Init")
        matchAll().excludes(controller: 'framework', action: '(createProject(Post)?|selectProject|projectSelect|noProjectAccess|(create|save|check|edit|view)ResourceModelConfig)')

        this.configurationService = configurationService
        this.projectAccessTrackingMap = new ConcurrentHashMap<>(200, 0.5F)

        // Set project notify period in milliseconds.
        this.minProjectNotifyPeriodMillis = 1000 * configurationService.getLong("audit.projectNotificationPeriod", DEFAULT_PROJECT_MIN_NOTIFICATION_PERIOD)
        this.cacheRetentionPeriodMillis = 1000 * configurationService.getLong("audit.minCacheRetentionPeriod", DEFAULT_CACHE_RETENTION_PERIOD)

        log.debug("Audit Interceptor initialized. ProjectNotify: " + minProjectNotifyPeriodMillis + " CacheRetention: " + cacheRetentionPeriodMillis)
    }


    boolean before() {

        if (InterceptorHelper.matchesStaticAssets(controllerName, request)) return true

        if (request.is_allowed_api_request || request.api_version || request.is_api_req) {
            //skip api calls
            return true
        }
        if (controllerName == 'user' && (actionName in ['logout', 'login'])) {
            // skip login & logout
            return true
        }

        // Check if audit service is operational
        if(!auditEventsService.enabled()) {
            return true
        }

        // Check project access tracking.
        if (!processProjectTracking()) {
            return false
        }

        // here we may call future checks

        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    /**
     * Tracks project access through the params.project request attribute already set by {@ProjectSelectInterceptor}
     * @return
     */
    private boolean processProjectTracking() {

        long now = System.currentTimeMillis()

        if (controllerName == 'menu' && (actionName in ['home'])) {
            return true
        }
        if (session && session.user && session.subject) {

            String requestProject = params.project

            // Skip logging on invalid project.
            if (!requestProject ||
                    !(requestProject =~ FrameworkResource.VALID_RESOURCE_NAME_REGEX) ||
                    !frameworkService.existsFrameworkProject(requestProject)) {
                return true
            }

            // Trigger project home access event
            if (log.isDebugEnabled()) {
                log.debug("Project Access: " + requestProject +
                        " User: " + session.user +
                        " Session: " + session.id +
                        " Controller: " + controllerName +
                        " Action: " + actionName
                )
            }

            String sessionId = session.id

            // Atomically set current tracking status for session.
            ProjectTrackingEntry computedEntry = projectAccessTrackingMap.compute(sessionId, { sid, curVal ->

                // If no entry, create new one.
                if (curVal == null) {
                    return new ProjectTrackingEntry(
                            sessionId: sid,
                            lastProject: requestProject,
                            lastSeen: now,
                            projectLastSeen: [(requestProject): now],
                            notify: true)
                }

                // Check project last notification
                def projectLastSeen = curVal.projectLastSeen.get(requestProject)
                if (projectLastSeen == null || (now - projectLastSeen) > minProjectNotifyPeriodMillis) {
                    // Only notify access if the min notification period has passed.
                    curVal.notify = true
                    curVal.projectLastSeen.put(requestProject, now)
                } else {
                    // Ignore access within the min notification period
                    curVal.notify = false
                }

                curVal.lastProject = requestProject
                curVal.lastSeen = now
                return curVal
            })


            if (computedEntry.notify) {
                // Notify audit service.
                auditEventsService.eventBuilder()
                        .setResourceType(ResourceTypes.PROJECT)
                        .setResourceName(computedEntry.lastProject)
                        .setActionType(ActionTypes.VIEW)
                        .publish()
            }
        }

        // Clear old entries if retention period has passed.
        if ((now - lastCachePrune) > cacheRetentionPeriodMillis) {
            pruneProjectTrackingCache()
        }

        return true
    }

    /**
     * Clears old entries from the cache.
     * @return
     */
    private pruneProjectTrackingCache() {
        log.debug("Pruning audit project tracking cache")
        long now = System.currentTimeMillis()

        projectAccessTrackingMap.values().removeIf {
            if ((now - it.lastSeen) > cacheRetentionPeriodMillis) {
                if (log.isDebugEnabled()) {
                    log.debug("Removing session entry: " + it.sessionId)
                }
                return true
            }
            return false
        }
        lastCachePrune = now
    }

    /**
     * Project Tracking Data Entry
     */
    private static class ProjectTrackingEntry {
        String sessionId
        long lastSeen
        String lastProject
        Map<String, Long> projectLastSeen
        boolean notify = false
    }

}

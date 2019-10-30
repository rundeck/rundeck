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
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.common.FrameworkResource

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

    private static final int DEFAULT_MIN_PROJECT_NOTIFY_MILLIS = 1000 * 30

    def frameworkService
    def auditEventsService
    Long minProjectNotifyPeriod = DEFAULT_MIN_PROJECT_NOTIFY_MILLIS
    Long cacheRetentionPeriod = DEFAULT_MIN_PROJECT_NOTIFY_MILLIS

    private final ConcurrentHashMap<String, ProjectTrackingEntry> projectAccessTrackingMap = new ConcurrentHashMap<>(10, 0.5F, 200)
    transient volatile long lastCachePrune = System.currentTimeMillis()

    AuditInterceptor() {
        log.debug("Audit Interceptor Init")
        matchAll().excludes(controller: 'framework', action: '(createProject(Post)?|selectProject|projectSelect|noProjectAccess|(create|save|check|edit|view)ResourceModelConfig)')
    }


    boolean before() {
        if (InterceptorHelper.matchesStaticAssets(controllerName, request)) return true

        if (request.is_allowed_api_request || request.api_version || request.is_api_req) {
            //only default the project if not an api request
            return true
        }
        if (controllerName == 'user' && (actionName in ['logout', 'login'])) {
            return true
        }

        // Process project access tracking.
        if (!processProjectTracking()) {
            return false
        }

        // here we may call future events

        return true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    /**
     * Tracks project access through the params.project session attribute.
     * @return
     */
    private boolean processProjectTracking() {

        long now = System.currentTimeMillis()

        if (controllerName == 'menu' && (actionName in ['home'])) {
            return true
        }
        if (session && session.user && session.subject) {
            //get user authorizations
            def AuthContext authContext = frameworkService.userAuthContext(session)

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
                if (curVal == null) {
                    return new ProjectTrackingEntry(
                            sessionId: sid,
                            lastProject: requestProject,
                            lastSeen: now,
                            projectLastSeen: [(requestProject): now],
                            notify: true)
                }

                def projectLastSeen = curVal.projectLastSeen.get(requestProject)
                if (projectLastSeen == null || (now - projectLastSeen) > minProjectNotifyPeriod) {
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

        // Clear tracking cache old entries if retention period has passed.
        if (now - lastCachePrune > cacheRetentionPeriod) {
            pruneProjectTrackingCache()
        }

        return true
    }

    /**
     * Clears old entries from the cache.
     * @return
     */
    private synchronized pruneProjectTrackingCache() {
        log.debug("Pruning audit project tracking cache")
        long now = System.currentTimeMillis()
        projectAccessTrackingMap.values().forEach {
            if (now - it.lastSeen > cacheRetentionPeriod) {
                projectAccessTrackingMap.remove(it.sessionId)
                if (log.isDebugEnabled()) {
                    log.debug("Removed session entry: " + it.sessionId)
                }
            }
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

/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import grails.events.annotation.Subscriber
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.services.scm.ProjectJobChangeListener

@Transactional
class JobEventsService {
    def List<JobChangeListener> listeners = []
    RundeckJobDefinitionManager rundeckJobDefinitionManager

    def addListener(JobChangeListener plugin) {
        listeners << plugin
    }
    /**
     * Adds a listener that only receives events for the given project
     * @param listener listener
     * @param project project
     * @return the instance of the listener that was added, can be used to call {@link #removeListener(com.dtolabs.rundeck.plugins.jobs.JobChangeListener)}
     */
    JobChangeListener addListenerForProject(JobChangeListener listener, String project) {
        def listener1 = new ProjectJobChangeListener(listener, project)
        listeners << listener1
        listener1
    }

    def removeListener(JobChangeListener plugin) {
        if (plugin != null) {
            listeners.remove(plugin)
        }
    }

    @Subscriber
    def multiJobChanged(List<StoredJobChangeEvent> e) {
        e.each(this.&jobChanged)
    }

    @Subscriber
    def jobChanged(StoredJobChangeEvent e) {
        if (!listeners) {
            return
        }
        JobSerializer serializer = null
        log.debug("job change: ${e.eventType} ${e.jobReference}")
        if (e.eventType != JobChangeEvent.JobChangeEventType.DELETE) {
            serializer = rundeckJobDefinitionManager.createJobSerializer(e.job)
        }
        listeners?.each { listener ->
            listener.jobChangeEvent(e, serializer)
        }
    }
}

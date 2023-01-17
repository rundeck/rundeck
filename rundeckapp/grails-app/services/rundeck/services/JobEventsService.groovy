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
import grails.gorm.transactions.Transactional
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.events.LogJobChangeEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import rundeck.services.scm.ProjectJobChangeListener

class JobEventsService {
    static Logger jobChangeLogger = LoggerFactory.getLogger("com.dtolabs.rundeck.data.jobs.changes")
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
    @Transactional
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

    @Subscriber('log.job.change.event')
    def logJobChange(LogJobChangeEvent e) {
        println "got a log job change event"
        def data = e.changeinfo
        data.keySet().each {k ->
            def v = data[k]
            if (v instanceof Date) {
                //TODO: reformat date
                MDC.put(k, v.toString())
                MDC.put("${k}Time", v.time.toString())
            } else if (v instanceof String) {
                MDC.put(k, v ? v : "-")
            } else {
                final string = v.toString()
                MDC.put(k, string ? string : "-")
            }
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            final var = e.jobData[k]
            MDC.put(k, var ? var.toString() : '-')
        }
        if (e.jobData.uuid) {
            MDC.put('id', e.jobData.uuid)
        }
        final msg = data.user + " " + data.change.toUpperCase() + " [" + (e.jobData.uuid ?: e.jobData.id) + "] " + e.jobData.project + " \"" + (e.jobData.groupPath ? e.jobData.groupPath : '') + "/" + e.jobData.jobName + "\" (" + data.method + ")"
        jobChangeLogger.info(msg)
        data.keySet().each {k ->
            if (data[k] instanceof Date) {
                //reformat date
                MDC.remove(k + 'Time')
            }
            MDC.remove(k)
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            MDC.remove(k)
        }
    }
}

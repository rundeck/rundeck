package rundeck.services

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import grails.events.Listener
import grails.transaction.Transactional
import rundeck.ScheduledExecution
import rundeck.services.scm.ProjectJobChangeListener

@Transactional
class JobEventsService {
    def List<JobChangeListener> listeners = []

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
        listeners.remove(plugin)
    }

    @Listener
    def jobChanged(StoredJobChangeEvent e) {
        if (!listeners) {
            return
        }
        JobSerializer serializer = null
        log.debug("job change: ${e.eventType} ${e.jobReference}")
        if (e.eventType != JobChangeEvent.JobChangeEventType.DELETE) {
            ScheduledExecution job = null
            String xmlString = null
            String yamlString = null
            int retry = 10
            while (retry > 0) {
                ScheduledExecution.withNewSession {
                    job = ScheduledExecution.getByIdOrUUID(e.jobReference.id)
                    if (job && job.version >= e.jobReference.version) {
                        retry = 0
                        //add line end char
                        xmlString = job.encodeAsJobsXML() + '\n'
                        yamlString = job.encodeAsJobsYAML() + '\n'
                    } else {
                        log.debug("did not receive updated job yet, waiting")
                    }
                }
                if (retry <= 0) {
                    break
                } else {
                    Thread.sleep(500)
                    retry--
                }
            }
            if (!job) {
                log.warn("JobChanged event: failed to load expected job changes, job data may be out of date")
            }
            serializer = new FromStringSerializer([xml: xmlString, yaml: yamlString])
        }
        listeners?.each { listener ->
            listener.jobChangeEvent(e, serializer)
        }
    }
}

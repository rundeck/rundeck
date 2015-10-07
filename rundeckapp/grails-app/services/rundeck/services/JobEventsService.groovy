package rundeck.services

import com.dtolabs.rundeck.plugins.jobs.JobChangeListener
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobSerializer
import grails.events.Listener
import grails.transaction.Transactional
import rundeck.ScheduledExecution

@Transactional
class JobEventsService {
    def List<JobChangeListener> listeners = []

    def addListener(JobChangeListener plugin) {
        listeners << plugin
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

                    if (!job) {
                        retry = 0
                        return
                    }
                    if (job?.version < e.jobReference.version) {
                        log.debug("did not receive updated job yet, waiting")
                    } else {
                        retry = 0
                        //add line end char
                        xmlString = job.encodeAsJobsXML() + '\n'
                        yamlString = job.encodeAsJobsYAML() + '\n'
                    }
                }
                if (retry <= 0) {

                    break
                } else {
                    Thread.sleep(500)
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

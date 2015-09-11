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
            ScheduledExecution job
            def xmlbytes
            def yamlbytes
            boolean  done=false
            while (!done) {
                ScheduledExecution.withNewSession {
                    job = ScheduledExecution.getByIdOrUUID(e.jobReference.id)

                    if (!job) {
                        done=true
                        return
                    }
                    if (job?.version < e.jobReference.version) {
                        log.error("did not receive updated job yet, waiting")
                    }else{
                        done=true
                        xmlbytes = job.encodeAsJobsXML().getBytes("UTF-8")
                        yamlbytes = job.encodeAsJobsYAML().getBytes("UTF-8")
                    }
                }
                if(done) {
                    break
                }else{
                    Thread.sleep(500)
                }
            }
            if (!job) {
                return
            }
            serializer = { String format, OutputStream os ->
                switch (format) {
                    case 'xml':
                        os.write(xmlbytes)
                        break;
                    case 'yaml':
                        os.write(yamlbytes)
                        break;
                    default:
                        throw new IllegalArgumentException("Format not supported: " + format)
                }
            }
        }
        listeners?.each { listener ->
            listener.jobChangeEvent(e, serializer)
        }
    }
}

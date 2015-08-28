package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobExportReference
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
        listeners?.each { listener ->
            JobSerializer serializer = null
            System.err.println("job change: ${e.eventType} ${e.jobReference}")
            if (e.eventType != JobChangeEvent.JobChangeEventType.DELETE) {
                ScheduledExecution job = ScheduledExecution.getByIdOrUUID(e.jobReference.id)
                serializer = {String format,OutputStream os->
                    switch(format){
                        case 'xml':
                            os.write(job.encodeAsJobsXML().getBytes("UTF-8"))
                            break;
                        case 'yaml':
                            os.write(job.encodeAsJobsYAML().getBytes("UTF-8"))
                            break;
                        default:
                            throw new IllegalArgumentException("Format not supported: "+format)
                    }
                }
            }
            listener.jobChangeEvent(e, serializer)
        }
    }
}

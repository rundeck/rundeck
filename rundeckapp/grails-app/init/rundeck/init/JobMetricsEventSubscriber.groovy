package rundeck.init

import grails.events.annotation.Subscriber
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import rundeck.Execution
import rundeck.services.JobMetricsSnapshotService

/**
 * Event subscribers for job metrics snapshot updates.
 * Subscribes to execution completion events and queues updates.
 */
@Slf4j
class JobMetricsEventSubscriber {

    @Autowired
    JobMetricsSnapshotService jobMetricsSnapshotService

    /**
     * Subscribe to execution completion events.
     * Queues update to job metrics snapshot.
     */
    @Subscriber('rundeck.execution.complete')
    void onExecutionComplete(Execution execution) {
        try {
            // Queue update (fast, non-blocking)
            jobMetricsSnapshotService.queueExecutionUpdate(execution)
        } catch (Exception e) {
            // Don't let metrics failures break execution completion
            log.error("[METRICS-SNAPSHOT] Failed to queue execution update for ${execution.id}", e)
        }
    }
}

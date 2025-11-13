package rundeckapp.init

import grails.events.annotation.Subscriber
import grails.events.bus.EventBusAware
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import rundeck.services.JobMetricsSnapshotService
import rundeck.services.events.ExecutionCompleteEvent

import javax.annotation.PostConstruct

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
    @Subscriber("executionComplete")
    void onExecutionComplete(ExecutionCompleteEvent event) {
        println("[METRICS-SNAPSHOT-EVENT-PRINTLN] Received executionComplete event for execution ${event.execution?.id}, job ${event.execution?.scheduledExecution?.id}")
        log.info("[METRICS-SNAPSHOT-EVENT] Received executionComplete event for execution ${event.execution?.id}, job ${event.execution?.scheduledExecution?.id}")
        try {
            // Queue update (fast, non-blocking)
            jobMetricsSnapshotService.queueExecutionUpdate(event.execution)
        } catch (Exception e) {
            // Don't let metrics failures break execution completion
            log.error("[METRICS-SNAPSHOT] Failed to queue execution update for ${event.execution?.id}", e)
        }
    }
}

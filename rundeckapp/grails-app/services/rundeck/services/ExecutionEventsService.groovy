package rundeck.services

import grails.events.Listener
import grails.transaction.Transactional
import rundeck.services.events.ExecutionCompleteEvent

@Transactional
class ExecutionEventsService {
    LogFileStorageService logFileStorageService

    /**
     * Prepares and submits logfile storage requests
     * @param event
     */
    @Listener
    def executionComplete(ExecutionCompleteEvent e) {

        logFileStorageService.submitForStorage(e.execution)
    }
}

package rundeck.services.workflow

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader
import com.dtolabs.rundeck.core.execution.workflow.state.StateExecutionFileProducer
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowStateDataLoader
import groovy.util.logging.Slf4j
import rundeck.services.LogFileStorageService

@Slf4j
class DefaultWorkflowStateDataLoader implements WorkflowStateDataLoader {
    LogFileStorageService logFileStorageService
    @Override
    ExecutionFileLoader loadWorkflowStateData(ExecutionReference executionReference, boolean performLoad) {
        log.debug("Loading workflow state data for execution $executionReference.id")
        return logFileStorageService.requestLogFileLoad(logFileStorageService.getExecutionByReferenceOrFail(executionReference),
                StateExecutionFileProducer.STATE_FILE_FILETYPE,
                performLoad)
    }
}

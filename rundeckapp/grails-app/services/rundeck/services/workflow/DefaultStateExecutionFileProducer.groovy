package rundeck.services.workflow

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.workflow.state.StateExecutionFileProducer
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState
import org.rundeck.app.services.ExecutionFile
import rundeck.services.WorkflowService
import rundeck.services.logging.ProducedExecutionFile

import java.nio.file.Files

class DefaultStateExecutionFileProducer implements StateExecutionFileProducer {

    WorkflowService workflowService

    @Override
    boolean isExecutionFileGenerated() {
        return false
    }

    @Override
    boolean isCheckpointable() {
        return true
    }

    @Override
    ExecutionFile produceStorageFileForExecution(final ExecutionReference e) {
        File localfile = workflowService.getStateFileForExecution(e)
        new ProducedExecutionFile(localFile: localfile,fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE)
    }

    @Override
    ExecutionFile produceStorageCheckpointForExecution(final ExecutionReference e) {
        long eid=Long.parseLong(e.id)
        File tempFile
        WorkflowState state = workflowService.getActiveState(eid)
        if (state) {
            tempFile = Files.createTempFile("WorkflowService-storage-${eid}", ".json").toFile()
            workflowService.persistExecutionState(eid, state, tempFile)
            return new ProducedExecutionFile(
                    localFile: tempFile,
                    fileDeletePolicy: ExecutionFile.DeletePolicy.ALWAYS
            )
        }
        File localfile = workflowService.getStateFileForExecution(e)

        def localproduced = new ProducedExecutionFile(
                localFile: localfile,
                fileDeletePolicy: ExecutionFile.DeletePolicy.WHEN_RETRIEVABLE
        )
        if (e.dateCompleted != null && localfile.exists()) {
            return localproduced
        }
        def statemap = workflowService.getCachedState(eid)
        if (statemap) {
            tempFile = Files.createTempFile("WorkflowService-storage-${eid}", ".json").toFile()
            workflowService.serializeStateDataJson(eid, statemap, tempFile)
            return new ProducedExecutionFile(
                    localFile: tempFile,
                    fileDeletePolicy: ExecutionFile.DeletePolicy.ALWAYS
            )
        }
        return localproduced
    }
}

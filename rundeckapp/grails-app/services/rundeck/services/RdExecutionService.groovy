package rundeck.services

import org.rundeck.app.data.model.v1.DeletionResult
import org.rundeck.app.data.model.v1.execution.ExecutionData
import org.rundeck.app.data.providers.v1.execution.ExecutionDataProvider

class RdExecutionService {

    ExecutionDataProvider executionDataProvider

    ExecutionData getExecutionByUuid(String uuid) {
        executionDataProvider.getByUuid(uuid)
    }

    ExecutionData saveExecutionData(ExecutionData executionData) {
        executionDataProvider.save(executionData)
    }

    DeletionResult delete(String id) {
        return executionDataProvider.delete(id)
    }
}

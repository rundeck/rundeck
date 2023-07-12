package org.rundeck.app.data.providers.v1.execution;

import org.rundeck.app.data.model.v1.DeletionResult;
import org.rundeck.app.data.model.v1.execution.ExecutionData;
import org.rundeck.app.data.model.v1.execution.ExecutionDataSummary;
import org.rundeck.app.data.model.v1.page.Pageable;
import org.rundeck.app.data.providers.v1.DataProvider;
import org.rundeck.spi.data.DataAccessException;

import java.io.Serializable;
import java.util.List;

public interface ExecutionDataProvider extends DataProvider {
    /**
     * Get execution data
     * @param id - the internal database id of the execution
     * @return execution data
     */
    ExecutionData get(Serializable id);

    /**
     * Get execution data
     * @param uuid - the uuid of the execution
     * @return execution data
     */
    ExecutionData getByUuid(String uuid);

    /**
     * Save execution data
     * @param execution
     * @return the saved execution data object
     * @throws DataAccessException
     */
    ExecutionData save(ExecutionData execution) throws DataAccessException;

    /**
     *
     * @param uuid execution uuid
     * @return result of the delete operation
     */
    DeletionResult delete(String uuid);

    /**
     *
     * @param jobUuid
     * @param pageable
     * @return a list of executions for the given job uuid
     */
    List<ExecutionDataSummary> findAllExecutionsByJob(String jobUuid, Pageable pageable);
}

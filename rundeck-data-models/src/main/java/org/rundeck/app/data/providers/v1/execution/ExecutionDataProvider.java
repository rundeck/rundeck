/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * @param execution - the execution data to save
     * @return the saved execution data object
     * @throws DataAccessException on error
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
     * @param jobUuid job uuid
     * @param pageable paging parameters
     * @return a list of executions for the given job uuid
     */
    List<ExecutionDataSummary> findAllExecutionsByJob(String jobUuid, Pageable pageable);
}

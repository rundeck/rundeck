/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package rundeck.services.data

import grails.gorm.services.Service
import rundeck.ScheduledExecution

@Service(ScheduledExecution)
abstract class ScheduledExecutionDataService implements IScheduledExecutionDataService {

    ScheduledExecution load(Serializable id) {
        ScheduledExecution.load(id)
    }

    /**
     * Batched lookup by uuid.
     *
     * Implemented here rather than left for the {@link Service} transform to generate: on GORM 9 the
     * data-service finder parser type-checks the argument against the property without accounting for
     * the {@code InList} operator, so it rejects {@code List<String>} against a {@code String} uuid.
     * The same finder is still generated normally on the domain class, which is what this delegates to.
     *
     * @param uuids job uuids to look up
     * @return the matching jobs, in no particular order
     */
    List<ScheduledExecution> findAllByUuidInList(List<String> uuids) {
        ScheduledExecution.findAllByUuidInList(uuids)
    }

}

interface IScheduledExecutionDataService {
    ScheduledExecution get(Serializable id)
    ScheduledExecution findByUuid(String uuid)
    List<ScheduledExecution> findAllByUuidInList(List<String> uuids)
    Integer countByUuid(String uuid)
}
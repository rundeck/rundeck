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
package org.rundeck.app.data.model.v1.query;

import org.rundeck.app.data.model.v1.page.Pageable;

import java.util.Map;

public interface JobQueryInputData extends Pageable {

    Map<String, Object> getInputParamMap();

    String getJobFilter();

    String getJobExactFilter();

    String getProjFilter();

    String getGroupPath();

    String getGroupPathExact();

    String getDescFilter();

    String getLoglevelFilter();

    String getIdlist();

    Boolean getScheduledFilter();

    Boolean getScheduleEnabledFilter();

    Boolean getExecutionEnabledFilter();

    String getServerNodeUUIDFilter();

    Integer getDaysAhead();

    Boolean getRunJobLaterFilter();

    default Boolean getPaginatedRequired(){return false;};
}

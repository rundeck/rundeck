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
package org.rundeck.app.data.model.v1.job.workflow;

import java.util.List;
import java.util.Map;

/**
 * Describes the workflow for a job
 */
public interface WorkflowData {
    /**
     *
     * @return Thread count to execute this workflow
     */
    Integer getThreadcount();

    /**
     *
     * @return Defines the workflow behavior when an error is encountered
     */
    Boolean getKeepgoing();

    /**
     *
     * @return The steps to run in the workflow
     */
    List<WorkflowStepData> getSteps();

    /**
     *
     * @return Defines the workflow strategy plugin to apply for this workflow
     */
    String getStrategy();

    /**
     *
     * @return Configuration data for the plugins attached to the workflow
     */
    Map<String,Object> getPluginConfigMap();

    /**
     *
     * @return Map representation of this workflow
     */
    Map<String, Object> toMap();
}

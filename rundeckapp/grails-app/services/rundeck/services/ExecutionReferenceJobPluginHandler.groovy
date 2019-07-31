/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package rundeck.services

import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.JobEventResult
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.JobPluginExecutionHandler
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl

class ExecutionReferenceJobPluginHandler implements JobPluginExecutionHandler {
    JobPluginService jobPluginService
    ExecutionReference executionReference
    List<NamedJobPlugin> plugins

    @Override
    Optional<JobEventStatus> beforeJobStarts(final StepExecutionContext executionContext) throws JobPluginException {
        Optional.ofNullable jobPluginService.handleEvent(
                new JobExecutionEventImpl(executionContext, executionReference),
                JobPluginService.EventType.BEFORE_RUN,
                plugins
        )

    }

    @Override
    Optional<JobEventStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws JobPluginException {
        Optional.ofNullable jobPluginService.handleEvent(
                new JobExecutionEventImpl(executionContext, executionReference, result),
                JobPluginService.EventType.AFTER_RUN,
                plugins
        )
    }
}

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
import com.dtolabs.rundeck.core.execution.ExecutionLifecyclePluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobEventResult
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.ExecutionLifecyclePluginHandler
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl

class ExecutionReferenceLifecyclePluginHandler implements ExecutionLifecyclePluginHandler {
    ExecutionLifecyclePluginService executionLifecyclePluginService
    ExecutionReference executionReference
    List<NamedExecutionLifecyclePlugin> plugins

    @Override
    Optional<JobEventStatus> beforeJobStarts(final StepExecutionContext executionContext, WorkflowExecutionItem item) throws ExecutionLifecyclePluginException {
        Optional.ofNullable executionLifecyclePluginService.handleEvent(
                JobExecutionEventImpl.beforeRun(executionContext, executionReference, item),
                ExecutionLifecyclePluginService.EventType.BEFORE_RUN,
                plugins
        )

    }

    @Override
    Optional<JobEventStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws ExecutionLifecyclePluginException {
        Optional.ofNullable executionLifecyclePluginService.handleEvent(
                JobExecutionEventImpl.afterRun(executionContext, executionReference, result),
                ExecutionLifecyclePluginService.EventType.AFTER_RUN,
                plugins
        )
    }
}

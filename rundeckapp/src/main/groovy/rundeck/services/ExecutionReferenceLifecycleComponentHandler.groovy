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
import com.dtolabs.rundeck.core.execution.ExecutionLifecycleComponentException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobEventResult
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleComponentHandler
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import groovy.transform.CompileStatic

/**
 * Handles execution lifecycle event calls using multiple plugins and an execution reference, via the {@link ExecutionLifecycleComponentService}
 */
@CompileStatic
class ExecutionReferenceLifecycleComponentHandler implements ExecutionLifecycleComponentHandler {
    ExecutionLifecycleComponentService executionLifecycleComponentService
    ExecutionReference executionReference
    List<NamedExecutionLifecycleComponent> components

    @Override
    Optional<ExecutionLifecycleStatus> beforeJobStarts(final StepExecutionContext executionContext, WorkflowExecutionItem item) throws ExecutionLifecycleComponentException {
        Optional.ofNullable executionLifecycleComponentService.handleEvent(
                JobExecutionEventImpl.beforeRun(executionContext, executionReference, item),
                ExecutionLifecycleComponentService.EventType.BEFORE_RUN,
                components
        )

    }

    @Override
    Optional<ExecutionLifecycleStatus> afterJobEnds(final StepExecutionContext executionContext, final JobEventResult result)
            throws ExecutionLifecycleComponentException {
        Optional.ofNullable executionLifecycleComponentService.handleEvent(
                JobExecutionEventImpl.afterRun(executionContext, executionReference, result),
                ExecutionLifecycleComponentService.EventType.AFTER_RUN,
                components
        )
    }
}

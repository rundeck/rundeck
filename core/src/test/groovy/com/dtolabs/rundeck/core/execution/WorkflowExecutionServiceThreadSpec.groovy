/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutor
import com.dtolabs.rundeck.core.execution.workflow.WorkflowItemErrorHandlerContext
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleComponentHandler
import spock.lang.Specification

class WorkflowExecutionServiceThreadSpec extends Specification {

    private static StepExecutionContext contextWithMarker() {
        ExecutionContextImpl.builder()
                .frameworkProject("prj")
                .stepContext([1])
                .stepNumber(1)
                .addComponent(
                        "WorkflowItemErrorHandlerContext",
                        WorkflowItemErrorHandlerContext.INSTANCE,
                        WorkflowItemErrorHandlerContext
                )
                .build()
    }

    private static boolean hasMarker(StepExecutionContext context) {
        context.componentForType(WorkflowItemErrorHandlerContext).present
    }

    def "the error handler marker is visible to lifecycle callbacks but not to the job's own workflow"() {
        // RUN-4837: the marker describes how this job was invoked (as an error handler). Lifecycle components
        // read it when the job starts, but the context their results are merged into keeps it, so it is
        // removed centrally before the workflow runs and cannot reach the job's own steps and nested references.
        given:
        boolean markerSeenByCallback = false
        StepExecutionContext executedWith = null
        def lifecycleHandler = Mock(ExecutionLifecycleComponentHandler) {
            beforeJobStarts(_, _) >> { args ->
                markerSeenByCallback = hasMarker(args[0])
                Optional.empty()
            }
        }
        def executor = Mock(WorkflowExecutor) {
            executeWorkflow(_, _) >> { args ->
                executedWith = args[0]
                Mock(WorkflowExecutionResult) {
                    isSuccess() >> true
                }
            }
        }
        def thread = new WorkflowExecutionServiceThread(
                Mock(WorkflowExecutionService) {
                    getExecutorForItem(_) >> executor
                },
                Mock(WorkflowExecutionItem),
                contextWithMarker(),
                null,
                lifecycleHandler
        )

        when:
        thread.runWorkflow()

        then:
        markerSeenByCallback
        executedWith != null
        !hasMarker(executedWith)
    }

    def "the error handler marker is removed before the workflow runs when there is no lifecycle handler"() {
        given:
        StepExecutionContext executedWith = null
        def executor = Mock(WorkflowExecutor) {
            executeWorkflow(_, _) >> { args ->
                executedWith = args[0]
                Mock(WorkflowExecutionResult) {
                    isSuccess() >> true
                }
            }
        }
        def thread = new WorkflowExecutionServiceThread(
                Mock(WorkflowExecutionService) {
                    getExecutorForItem(_) >> executor
                },
                Mock(WorkflowExecutionItem),
                contextWithMarker(),
                null,
                null
        )

        when:
        thread.runWorkflow()

        then:
        executedWith != null
        !hasMarker(executedWith)
    }
}

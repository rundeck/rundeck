/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow.engine;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.BaseWorkflowExecutor;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeExecutionContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Execute a workflow step
 * @author greg
 * @since 4/28/17
 */
class StepCallable implements Function<WFSharedContext, BaseWorkflowExecutor.StepResultCapture> {
    private EngineWorkflowExecutor engineWorkflowExecutor;
    private final StepExecutionContext executionContext;
    private final boolean keepgoing;
    private final WorkflowExecutionListener wlistener;
    private final int i;
    private final StepExecutionItem cmd;

    public StepCallable(
            final EngineWorkflowExecutor engineWorkflowExecutor,
            final StepExecutionContext executionContext,
            final boolean keepgoing,
            final WorkflowExecutionListener wlistener,
            final int i,
            final StepExecutionItem cmd
    )
    {
        this.engineWorkflowExecutor = engineWorkflowExecutor;
        this.executionContext = executionContext;
        this.keepgoing = keepgoing;
        this.wlistener = wlistener;
        this.i = i;
        this.cmd = cmd;
    }

    @Override
    public BaseWorkflowExecutor.StepResultCapture apply(final WFSharedContext inputData) {
        executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Input data context: " + inputData);
        StepExecutionContext newContext =
                ExecutionContextImpl.builder(executionContext)
                                    .sharedDataContext(inputData)
                                    .build();

        final Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<>();
        List<StepExecutionResult> resultList = new ArrayList<>();
        try {
            return engineWorkflowExecutor.executeWorkflowStep(
                    newContext,
                    stepFailedMap,
                    resultList,
                    keepgoing,
                    wlistener,
                    i,
                    cmd
            );
        } catch (Throwable e) {
            String message = String.format(
                    "Exception while executing step [%d]: [%s]",
                    i,
                    e.toString()
            );
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
            throw new RuntimeException(e);
        }
    }
}

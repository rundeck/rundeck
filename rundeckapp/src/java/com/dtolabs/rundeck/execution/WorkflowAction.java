/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.ExecutionResult;
import com.dtolabs.rundeck.core.execution.ExecutionService;

/**
 * An action to execute a workflow execution item within a context
 */
public class WorkflowAction {
    final Framework framework;
    final ExecutionService executionService;
    final WorkflowExecutionItem item;
    final ExecutionListener listener;
    final WorkflowStrategy strategy;
    public static final String NODE_FIRST = "node-first";
    public static final String STEP_FIRST = "step-first";

    public WorkflowAction(final Framework framework, final ExecutionService executionService, final WorkflowExecutionItem item,
                          final ExecutionListener listener) throws WorkflowFailureException {
        this.framework = framework;
        this.executionService = executionService;
        this.item = item;
        this.listener = listener;
        if (NODE_FIRST.equals(item.getWorkflow().getStrategy())) {
            strategy = new NodeFirstWorkflowStrategy(item, executionService, listener, framework);
        }else if(STEP_FIRST.equals(item.getWorkflow().getStrategy())){
            strategy = new StepFirstWorkflowStrategy(item,executionService,listener, framework);
        }else {
            throw new WorkflowFailureException("Invalid strategy specified: " + item.getWorkflow().getStrategy());
        }
    }

    public ExecutionResult executeWorkflow() {
        return strategy.executeWorkflow();

    }
    public static class WorkflowFailureException extends ExecutionException{
        public WorkflowFailureException(String s) {
            super(s);
        }
    }


    public static class WorkflowStepFailureException extends Exception {
        final private ExecutionResult executionResult;
        final private int workflowStep;

        public WorkflowStepFailureException(final String s, final ExecutionResult executionResult, final int workflowStep) {
            super(s);
            this.executionResult = executionResult;
            this.workflowStep = workflowStep;
        }

        public WorkflowStepFailureException(final String s, final Throwable throwable, final int workflowStep) {
            super(s, throwable);
            this.executionResult = null;
            this.workflowStep = workflowStep;
        }

        public ExecutionResult getExecutionResult() {
            return executionResult;
        }

        public int getWorkflowStep() {
            return workflowStep;
        }
    }
}
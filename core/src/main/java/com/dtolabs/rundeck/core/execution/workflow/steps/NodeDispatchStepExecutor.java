/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* NodeDispatchStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/2/12 10:47 AM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;


/**
 * NodeDispatchStepExecutor dispatches the step execution item to all nodes, via the ExecutionService
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatchStepExecutor implements StepExecutor {
    public static final String STEP_EXECUTION_TYPE = "NodeDispatch";

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        return true;
    }

    @Override
    public StepExecutionResult executeWorkflowStep(final ExecutionContext context,
                                                   final StepExecutionItem executionItem) {
        if (!(executionItem instanceof NodeStepExecutionItem)) {
            throw new IllegalArgumentException(
                "Cannot executeWorkflowStep: step is not a NodeStepExecutionItem: " + executionItem);
        }
        NodeStepExecutionItem item = (NodeStepExecutionItem) executionItem;

        final Framework framework = context.getFramework();
        try {
            return wrapDispatcherResult(framework.getExecutionService().dispatchToNodes(context, item));
        } catch (DispatcherException e) {
            return wrapDispatcherException(e);
        }
    }

    /**
     * Return a StepExecutionResult based on the DispatcherResult, that can later be extracted.
     */
    public static StepExecutionResult wrapDispatcherException(final DispatcherException dispatcherResult) {
        final StepExecutionResultImpl result = new NodeDispatchStepExecutorExceptionResult(dispatcherResult);
        return result;
    }
    /**
     * Return a StepExecutionResult based on the DispatcherResult, that can later be extracted.
     */
    public static StepExecutionResult wrapDispatcherResult(final DispatcherResult dispatcherResult) {
        final StepExecutionResultImpl result = new NodeDispatchStepExecutorResult(dispatcherResult.isSuccess(),
                                                                            dispatcherResult);
        result.setSourceResult(dispatcherResult);
        return result;
    }

    static class NodeDispatchStepExecutorResult extends StepExecutionResultImpl{
        DispatcherResult dispatcherResult;

        NodeDispatchStepExecutorResult(final boolean success, DispatcherResult dispatcherResult) {
            super(success);
            this.dispatcherResult = dispatcherResult;
            setSourceResult(dispatcherResult);
        }
        public DispatcherResult getDispatcherResult(){
            return dispatcherResult;
        }
    }
    static class NodeDispatchStepExecutorExceptionResult extends StepExecutionResultImpl{
        DispatcherException dispatcherResult;

        NodeDispatchStepExecutorExceptionResult(DispatcherException dispatcherResult) {
            super(false);
            this.dispatcherResult = dispatcherResult;
            setException(dispatcherResult);
        }
        public DispatcherException getDispatcherException(){
            return dispatcherResult;
        }
    }

    public static boolean isWrappedDispatcherException(final StepExecutionResult result) {
        return (result instanceof NodeDispatchStepExecutorExceptionResult);
    }
    public static boolean isWrappedDispatcherResult(final StepExecutionResult result) {
        return (result instanceof NodeDispatchStepExecutorResult);
    }
    /**
     * Return the DispatcherResult from a StepExecutionResult created by this class.
     */
    public static DispatcherResult extractDispatcherResult(final StepExecutionResult result) {
        if(!isWrappedDispatcherResult(result)) {
            throw new IllegalArgumentException("Cannot extract result: unexpected type: " + result);
        }
        NodeDispatchStepExecutorResult nr = (NodeDispatchStepExecutorResult) result;
        return nr.getDispatcherResult();
    }
    /**
     * Return the DispatcherResult from a StepExecutionResult created by this class.
     */
    public static DispatcherException extractDispatcherException(final StepExecutionResult result) {
        if(!isWrappedDispatcherException(result)) {
            throw new IllegalArgumentException("Cannot extract result: unexpected type: " + result);
        }
        NodeDispatchStepExecutorExceptionResult nr = (NodeDispatchStepExecutorExceptionResult) result;
        return nr.getDispatcherException();
    }
}

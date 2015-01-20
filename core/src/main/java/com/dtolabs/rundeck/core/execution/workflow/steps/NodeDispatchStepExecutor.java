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

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;


/**
 * NodeDispatchStepExecutor dispatches the step execution item to all nodes, via the ExecutionService
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeDispatchStepExecutor implements StepExecutor {
    public static final String STEP_EXECUTION_TYPE = "NodeDispatch";
    public static final String FAILURE_DATA_FAILED_NODES = "failedNodes";

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        return true;
    }

    @Override
    public StepExecutionResult executeWorkflowStep(final StepExecutionContext context,
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
        } catch (ExecutionServiceException e) {
            //internal error if failure using node dispatchers
            throw new CoreException(e);
        }
    }

    static enum Reason implements FailureReason{
        /**
         * Node dispatch failed on at least one node
         */
        NodeDispatchFailure
    }
    /**
     * Return a StepExecutionResult based on the DispatcherResult, that can later be extracted.
     * @param dispatcherResult exception result
     * @return step result
     */
    public static StepExecutionResult wrapDispatcherException(final DispatcherException dispatcherResult) {
        final StepExecutionResultImpl result = new NodeDispatchStepExecutorExceptionResult(dispatcherResult,
                                                                                           Reason.NodeDispatchFailure,
                                                                                           dispatcherResult.getMessage());

        return result;
    }

    /**
     * Return a StepExecutionResult based on the DispatcherResult, that can later be extracted.
     * @param dispatcherResult result
     * @return step result
     */
    public static StepExecutionResult wrapDispatcherResult(final DispatcherResult dispatcherResult) {
        final StepExecutionResultImpl result;
        if(dispatcherResult.isSuccess()) {
            result = NodeDispatchStepExecutorResult.success(dispatcherResult);
        }else{
            result = NodeDispatchStepExecutorResult.failure(dispatcherResult,
                                                            null,
                                                            Reason.NodeDispatchFailure,
                                                            "Node dispatch failed");
            //extract failed nodes
            ArrayList<String> nodeNames = new ArrayList<String>();
            for (String nodeName : dispatcherResult.getResults().keySet()) {
                NodeStepResult nodeStepResult = dispatcherResult.getResults().get(nodeName);
                if(!nodeStepResult.isSuccess()) {
                    nodeNames.add(nodeName);
                }
            }
            if(!nodeNames.isEmpty()){
                result.getFailureData().put(FAILURE_DATA_FAILED_NODES, StringUtils.join(nodeNames, ","));
            }
        }
        return result;
    }

    static class NodeDispatchStepExecutorResult extends StepExecutionResultImpl{
        DispatcherResult dispatcherResult;
        static NodeDispatchStepExecutorResult success(DispatcherResult dispatcherResult) {
            return new NodeDispatchStepExecutorResult(dispatcherResult);
        }
        static NodeDispatchStepExecutorResult failure(DispatcherResult dispatcherResult,
                                                      final Exception exception,
                                                      final FailureReason reason, final String message){
            return new NodeDispatchStepExecutorResult(dispatcherResult, exception, reason, message);
        }
        NodeDispatchStepExecutorResult(DispatcherResult dispatcherResult) {
            super();
            this.dispatcherResult = dispatcherResult;
            setSourceResult(dispatcherResult);
        }
        NodeDispatchStepExecutorResult(DispatcherResult dispatcherResult,
                                       final Exception exception, final FailureReason reason, final String message) {
            super(exception, reason, message);
            this.dispatcherResult = dispatcherResult;
            setSourceResult(dispatcherResult);
        }
        public DispatcherResult getDispatcherResult(){
            return dispatcherResult;
        }

        @Override
        public String toString() {
            if (null!=dispatcherResult) {
                return dispatcherResult.toString();
            } else {
                return super.toString();
            }
        }

    }
    static class NodeDispatchStepExecutorExceptionResult extends StepExecutionResultImpl{
        DispatcherException dispatcherResult;

        NodeDispatchStepExecutorExceptionResult(DispatcherException dispatcherResult,
                                                final FailureReason reason, final String message) {
            super(dispatcherResult, reason, message);
            this.dispatcherResult = dispatcherResult;
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
     * @param result step result
     * @return dispatcher result
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
     * @param result step exception
     * @return dispatcher exception
     */
    public static DispatcherException extractDispatcherException(final StepExecutionResult result) {
        if(!isWrappedDispatcherException(result)) {
            throw new IllegalArgumentException("Cannot extract result: unexpected type: " + result);
        }
        NodeDispatchStepExecutorExceptionResult nr = (NodeDispatchStepExecutorExceptionResult) result;
        return nr.getDispatcherException();
    }
}

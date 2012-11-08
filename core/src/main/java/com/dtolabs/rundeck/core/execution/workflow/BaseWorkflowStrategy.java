/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* BaseWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:19:17 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.SelectorUtils;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.HasDispatcherResult;

import java.util.*;

/**
 * BaseWorkflowStrategy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public abstract class BaseWorkflowStrategy implements WorkflowStrategy {
    final Framework framework;

    public BaseWorkflowStrategy(final Framework framework) {
        this.framework = framework;
    }

    static class WorkflowExecutionResult implements
        com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult {
        private final HashMap<String, List<StatusResult>> results;
        private final Map<String, Collection<String>> failures;
        private final boolean success;
        private final Exception orig;

        public WorkflowExecutionResult(HashMap<String, List<StatusResult>> results,
                                       Map<String, Collection<String>> failures,
                                       boolean success, Exception orig) {
            this.results = results;
            this.failures = failures;
            this.success = success;
            this.orig = orig;
        }

        public Map<String, List<StatusResult>> getResultSet() {
            return results;
        }

        public Map<String, Collection<String>> getFailureMessages() {
            return failures;
        }

        public boolean isSuccess() {
            return success;
        }

        public Exception getException() {
            return orig;
        }

        @Override
        public String toString() {
            return "[Workflow "
                   + (null != getResultSet() && getResultSet().size() > 0 ? "results: " + getResultSet() : "")
                   + (null != getFailureMessages() && getFailureMessages().size() > 0 ? ", failures: "
                                                                                        + getFailureMessages() : "")
                   + (null != getException() ? ": exception: " + getException() : "")
                   + "]";
        }

    }

    public final WorkflowExecutionResult executeWorkflow(final ExecutionContext executionContext,
                                                         final WorkflowExecutionItem item) {

        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        if (null != wlistener && !StepFirstWorkflowStrategy.isInnerLoop(item)) {
            wlistener.beginWorkflowExecution(executionContext, item);
        }
        WorkflowExecutionResult result = null;
        try {
            result = executeWorkflowImpl(executionContext, item);
        } finally {
            if (null != wlistener && !StepFirstWorkflowStrategy.isInnerLoop(item)) {
                wlistener.finishWorkflowExecution(result, executionContext, item);
            }
        }
        return result;
    }

    private WorkflowExecutionListener getWorkflowListener(final ExecutionContext executionContext) {
        WorkflowExecutionListener wlistener = null;
        final ExecutionListener elistener = executionContext.getExecutionListener();
        if (null != elistener && elistener instanceof WorkflowExecutionListener) {
            wlistener = (WorkflowExecutionListener) elistener;
        }
        return wlistener;
    }

    public abstract WorkflowExecutionResult executeWorkflowImpl(ExecutionContext executionContext,
                                                                WorkflowExecutionItem item);

    /**
     * Execute a workflow item, returns true if the item succeeds.  This method will throw an exception if the workflow
     * item fails and the Workflow is has keepgoing==false.
     *
     * @param failedMap  List to add any messages if the item fails
     * @param resultList List to add any Objects that are results of execution
     * @param c          index of the WF item
     * @param cmd        WF item descriptor
     * @param keepgoing
     *
     * @return true if the execution succeeds, false otherwise
     *
     * @throws WorkflowStepFailureException if underlying WF item throws exception and the workflow is not "keepgoing",
     *                                      or the result from the execution includes an exception
     */
    protected boolean executeWFItem(final ExecutionContext executionContext,
                                    final Map<Integer, Object> failedMap,
                                    final List<DispatcherResult> resultList,
                                    final int c,
                                    final StepExecutionItem cmd, final boolean keepgoing) throws
        WorkflowStepFailureException {

        if(null!=executionContext.getExecutionListener()){
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, c + ": " + cmd.toString());
        }
        StatusResult result = null;
        boolean itemsuccess;
        Throwable wfstepthrowable = null;
        try {
            if(null!=executionContext.getExecutionListener()){
                executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                "StepExecutionItem created, executing: " + cmd);
            }
            result = framework.getExecutionService().executeStep(executionContext, cmd);
            itemsuccess = null != result && result.isSuccess();
        } catch (Throwable exc) {
            if (keepgoing) {
                //don't fail
                if (null != executionContext.getExecutionListener()) {
                executionContext.getExecutionListener().log(Constants.VERBOSE_LEVEL,
                    "Step " + c + "of the workflow failed: " + org.apache.tools.ant.util
                        .StringUtils.getStackTrace(exc));
                }
                wfstepthrowable = exc;
                itemsuccess = false;
            } else {

                failedMap.put(c, exc.getMessage());
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw exception: " + exc.getMessage(), exc, c);
            }
        }
        DispatcherResult dispatcherResult=null;
        if (null != result && (result instanceof HasDispatcherResult)) {
            dispatcherResult = ((HasDispatcherResult) result).getDispatcherResult();
            resultList.add(dispatcherResult);
        }
        Exception exception=null;
        if(null!=result && (result instanceof ExceptionStatusResult)) {
            exception = ((ExceptionStatusResult) result).getException();
        }
        if (itemsuccess) {
            if (null != executionContext.getExecutionListener()) {
                executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                            c + ": StepExecutionItem finished, result: " + result);
            }
        } else if (keepgoing) {
            //don't fail yet
            failedMap.put(c, (null != wfstepthrowable ? wfstepthrowable.getMessage()
                                                      : (null != result && null != exception ? exception
                                                                                             : (null != result && null
                                                                                                                  != dispatcherResult
                                                                                                ? dispatcherResult
                                                                                                : "no result"))));
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Workflow continues");
        } else {
            failedMap.put(c, (null != wfstepthrowable ? wfstepthrowable.getMessage()
                                                      : (null != result && null != exception ? exception
                                                                                             : (null != result && null
                                                                                                                  != dispatcherResult
                                                                                                ? dispatcherResult
                                                                                                : "no result"))));
            if (null != result && null != exception) {
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw an exception: " + exception.getMessage(),
                    exception, c);
            } else {
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow failed with result: " + (result != null ? result : null),
                    result,
                    c);
            }
        }
        return itemsuccess;
    }




    /**
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value, return true if
     * successful
     */
    protected boolean executeWorkflowItemsForNodeSet(final ExecutionContext executionContext,
                                                     final Map<Integer, Object> failedMap,
                                                     final List<DispatcherResult> resultList,
                                                     final List<StepExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing) throws
        WorkflowStepFailureException {
        return executeWorkflowItemsForNodeSet(executionContext, failedMap, resultList, iWorkflowCmdItems, keepgoing, 1);
    }
    /**
     * Execute the sequence of ExecutionItems within the context, and with the given keepgoing value, return true if
     * successful
     */
    protected boolean executeWorkflowItemsForNodeSet(final ExecutionContext executionContext,
                                                     final Map<Integer, Object> failedMap,
                                                     final List<DispatcherResult> resultList,
                                                     final List<StepExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing,
                                                     final int beginStepIndex) throws
        WorkflowStepFailureException {

        boolean workflowsuccess = true;
        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        int c = beginStepIndex;
        for (final StepExecutionItem cmd : iWorkflowCmdItems) {
            boolean stepSuccess=false;
            WorkflowStepFailureException stepFailure=null;
            if (null != wlistener) {
                wlistener.beginWorkflowItem(c, cmd);
            }

            //wrap node failed listener (if any) and capture status results
            NodeRecorder stepCaptureFailedNodesListener = new NodeRecorder();
            ExecutionContext stepContext = replaceFailedNodesListenerInContext(executionContext,
                stepCaptureFailedNodesListener);
            Map<String,Object> nodeFailures;

            //execute the step item, and store the results
            ArrayList<DispatcherResult> stepResult = new ArrayList<DispatcherResult>();
            Map<Integer, Object> stepFailedMap = new HashMap<Integer, Object>();
            try {
                stepSuccess = executeWFItem(stepContext, stepFailedMap, stepResult, c, cmd, keepgoing);
            } catch (WorkflowStepFailureException e) {
                stepFailure = e;
            }
            nodeFailures = stepCaptureFailedNodesListener.getFailedNodes();

            if(null!=executionContext.getExecutionListener() && null!=executionContext.getExecutionListener().getFailedNodesListener()) {
                executionContext.getExecutionListener().getFailedNodesListener().matchedNodes(
                    stepCaptureFailedNodesListener.getMatchedNodes());

            }

            try {
                if(!stepSuccess && cmd instanceof HasFailureHandler) {
                    final HasFailureHandler handles = (HasFailureHandler) cmd;
                    final StepExecutionItem handler = handles.getFailureHandler();
                    if (null != handler) {
                        //if there is a failure, and a failureHandler item, execute the failure handler
                        //set keepgoing=false, and store the results
                        //will throw an exception on failure because keepgoing=false

                        NodeRecorder handlerCaptureFailedNodesListener = new NodeRecorder();
                        ExecutionContext handlerExecContext = replaceFailedNodesListenerInContext(executionContext,
                            handlerCaptureFailedNodesListener);

                        //if multi-node, determine set of nodes to run handler on: (failed node list only)
                        if(stepCaptureFailedNodesListener.getMatchedNodes().size()>1) {
                            HashSet<String> failedNodeList = new HashSet<String>(
                                stepCaptureFailedNodesListener.getFailedNodes().keySet());

                            handlerExecContext = new ExecutionContextImpl.Builder(handlerExecContext).nodeSelector(
                                SelectorUtils.nodeList(failedNodeList)).build();

                        }

                        ArrayList<DispatcherResult> handlerResult = new ArrayList<DispatcherResult>();
                        Map<Integer, Object> handlerFailedMap = new HashMap<Integer, Object>();
                        WorkflowStepFailureException handlerFailure = null;
                        boolean handlerSuccess = false;
                        try {
                            handlerSuccess = executeWFItem(handlerExecContext, handlerFailedMap, handlerResult, c,
                                                           handler,
                                false);
                        } catch (WorkflowStepFailureException e) {
                            handlerFailure = e;
                        }

                        //handle success conditions:
                        //1. if keepgoing=true, then status from handler overrides original step
                        //2. keepgoing=false, then status is the same as the original step, unless
                        //   the keepgoingOnSuccess is set to true and the handler succeeded
                        if (keepgoing) {
                            stepSuccess = handlerSuccess;
                            stepFailure = handlerFailure;
                            stepResult.addAll(handlerResult);
                            stepFailedMap = handlerFailedMap;
                            nodeFailures = handlerCaptureFailedNodesListener.getFailedNodes();
                        }else if(handlerSuccess && handler instanceof HandlerExecutionItem) {
                            final boolean keepgoingOnSuccess
                                = ((HandlerExecutionItem) handler).isKeepgoingOnSuccess();
                            if(keepgoingOnSuccess){
                                stepSuccess = handlerSuccess;
                                stepFailure = handlerFailure;
                                stepResult.addAll(handlerResult);
                                stepFailedMap = handlerFailedMap;
                                nodeFailures = handlerCaptureFailedNodesListener.getFailedNodes();
                            }
                        }
                    }
                }
            } finally {
                if (null != wlistener) {
                    wlistener.finishWorkflowItem(c, cmd);
                }
            }
            resultList.addAll(stepResult);
            failedMap.putAll(stepFailedMap);
            if(!stepSuccess){
                workflowsuccess = false;
            }

            //report node failures based on results of step and handler run.
            if (null != executionContext.getExecutionListener() && null != executionContext.getExecutionListener()
                .getFailedNodesListener()) {
                if(nodeFailures.size()>0){
                    executionContext.getExecutionListener().getFailedNodesListener().nodesFailed(
                    nodeFailures);
                }else if(workflowsuccess){
                    executionContext.getExecutionListener().getFailedNodesListener().nodesSucceeded();
                }

            }

            if(null!=stepFailure && !keepgoing){
                throw stepFailure;
            }else if(!stepSuccess && !keepgoing){
                break;
            }
            c++;
        }
        return workflowsuccess;
    }
    private class noopExecutionListener extends ExecutionListenerOverrideBase{
        public noopExecutionListener(noopExecutionListener delegate){
            super(delegate);
        }
        public void log(int level, String message) {
        }

        public ExecutionListenerOverride createOverride() {
            return new noopExecutionListener(this);
        }
    }

    private ExecutionContext replaceFailedNodesListenerInContext(ExecutionContext executionContext,
                                                                 FailedNodesListener captureFailedNodesListener) {
        ExecutionListenerOverride listen=null;
        if(null!= executionContext.getExecutionListener()) {
            listen = executionContext.getExecutionListener().createOverride();
        }
        if(null!=listen){
            listen.setFailedNodesListener(captureFailedNodesListener);
        }

        return new ExecutionContextImpl.Builder(executionContext).executionListener(listen).build();
    }

    /**
     * Convert list of DispatcherResult items to map of Node name to Map of NodeStepResult items keyed by index in
     * the list (0-first)
     *
     * @param resultList dispatcher result list
     *
     * @return map of node name to Map of NodeStepResult items keyed by index in the list (0-first)
     */
    protected HashMap<String, List<StatusResult>> convertResults(final List<DispatcherResult> resultList) {
        final HashMap<String, List<StatusResult>> results = new HashMap<String, List<StatusResult>>();
        //iterate resultSet and place in map
        int i = 0;
        for (final DispatcherResult dispatcherResult : resultList) {
            for (final String s : dispatcherResult.getResults().keySet()) {
                final StatusResult interpreterResult = dispatcherResult.getResults().get(s);
                if (!results.containsKey(s)) {
                    results.put(s, new ArrayList<StatusResult>());
                }
                results.get(s).add(interpreterResult);
            }
            i++;
        }
        return results;
    }

    /**
     * Convert map of integer to failure object to map of node name to collection o string.
     */
    protected Map<String, Collection<String>> convertFailures(Map<Integer, Object> failedMap) {
        final Map<String, Collection<String>> failures = new HashMap<String, Collection<String>>();
        for (final Map.Entry<Integer, Object> entry : failedMap.entrySet()) {
            final Object o = entry.getValue();
            if (o instanceof DispatcherResult) {
                //indicates dispatcher returned node results
                DispatcherResult dispatcherResult = (DispatcherResult) o;

                for (final String s : dispatcherResult.getResults().keySet()) {
                    final StatusResult interpreterResult = dispatcherResult.getResults().get(s);
                    if (!failures.containsKey(s)) {
                        failures.put(s, new ArrayList<String>());
                    }
                    failures.get(s).add(interpreterResult.toString());
                }
            } else if (o instanceof DispatcherException) {
                DispatcherException e = (DispatcherException) o;
                final INodeEntry node = e.getNode();
                final String key = null != node ? node.getNodename() : "?";
                if (!failures.containsKey(key)) {
                    failures.put(key, new ArrayList<String>());
                }
                failures.get(key).add(e.getMessage());
            } else if (o instanceof Exception) {
                Exception e = (Exception) o;
                if (!failures.containsKey("?")) {
                    failures.put("?", new ArrayList<String>());
                }
                failures.get("?").add(e.getMessage());
            } else {
                if (!failures.containsKey("?")) {
                    failures.put("?", new ArrayList<String>());
                }
                failures.get("?").add(o.toString());
            }
        }
        return failures;
    }
}

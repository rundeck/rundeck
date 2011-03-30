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
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.*;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;

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

    public InterpreterResult interpretCommand(ExecutionContext executionContext,
                                              ExecutionItem executionItem, INodeEntry iNodeEntry) throws
        InterpreterException {
        if (!(executionItem instanceof WorkflowExecutionItem)) {
            throw new InterpreterException("Incorrect item type: " + executionItem.getClass().getName());
        }
        final WorkflowExecutionItem item = (WorkflowExecutionItem) executionItem;
        final WorkflowExecutionResult workflowExecutionResult = executeWorkflow(executionContext, item);
        return new StrategyInterpreterResult(workflowExecutionResult);
    }

    static class StrategyInterpreterResult implements InterpreterResult {
        private final WorkflowExecutionResult workflowExecutionResult;

        StrategyInterpreterResult(WorkflowExecutionResult workflowExecutionResult) {
            this.workflowExecutionResult = workflowExecutionResult;
        }

        public boolean isSuccess() {
            return workflowExecutionResult.isSuccess();
        }

        public WorkflowExecutionResult getWorkflowExecutionResult() {
            return workflowExecutionResult;
        }

        @Override
        public String toString() {
            return "Workflow: " + (isSuccess() ? "success" : "failure") + ", result: " + getWorkflowExecutionResult();
        }
    }


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
                                    final ExecutionItem cmd, final boolean keepgoing) throws
        WorkflowStepFailureException {
        //TODO evaluate conditionals set for cmd within the data context, and skip cmd if necessary
        executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, c + ": " + cmd.toString());
        ExecutionResult result = null;
        boolean itemsuccess;
        Throwable wfstepthrowable = null;
        try {
//            final ArrayList<String> argslist = new ArrayList<String>();
//            if (null == cmd.getAdhocRemoteString() && null != cmd.getArgString()) {
//                argslist.addAll(CLIUtils.splitArgLine(cmd.getArgString()));
//            }
//            final String[] args = argslist.toArray(new String[argslist.size()]);

            /*final ExecutionContext context=new ExecutionContext(){
                public String getFrameworkProject() {
                    return executionContext.getFrameworkProject();
                }

                public String getUser() {
                    return executionContext.getUser();
                }

                public NodeSet getNodeSet() {
                    return executionContext.getNodeSet();
                }

                public String[] getArgs() {
                    return args;
                }

                public int getLoglevel() {
                    return executionContext.getLoglevel();
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return executionContext.getDataContext();
                }

                public ExecutionListener getExecutionListener() {
                    return executionContext.getExecutionListener();
                }
            };*/
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                "ExecutionItem created, executing: " + cmd);
            result = framework.getExecutionService().executeItem(executionContext, cmd);
            itemsuccess = null != result && result.isSuccess();
        } catch (Throwable exc) {
            if (keepgoing) {
                //don't fail
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL,
                    c + ": wf item failed: " + exc.getMessage());
                executionContext.getExecutionListener().log(Constants.VERBOSE_LEVEL,
                    c + ": wf item failed: " + org.apache.tools.ant.util
                        .StringUtils.getStackTrace(exc));
                wfstepthrowable = exc;
                itemsuccess = false;
            } else {
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL,
                    c + ": wf item failed: " + exc.getMessage());
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw exception: " + exc.getMessage(), exc, c);
            }
        }
        //TODO: evaluate result object and set result data into the data context
        if (null != result && null != result.getResultObject()) {
            resultList.add(result.getResultObject());
        }

        if (itemsuccess) {
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                c + ": ExecutionItem finished, result: " + result);
        } else if (keepgoing) {
            //don't fail yet
            failedMap.put(c, (null != wfstepthrowable ? wfstepthrowable.getMessage()
                                                      : (null != result && null != result.getException() ? result
                                                          .getException() : (null != result && null != result
                                                          .getResultObject() ? result.getResultObject()
                                                                             : "no result"))));
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Workflow continues");
        } else {
            if (null != result && null != result.getException()) {
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw an exception: " + result.getException().getMessage(),
                    result.getException(), c);
            } else {
                throw new WorkflowStepFailureException(
                    "Step " + c + " of the workflow failed with result: " + (result != null ? result
                        .getResultObject() : null), result, c);
            }
        }
        return itemsuccess;
    }

    protected boolean executeWorkflowItemsForNodeSet(final ExecutionContext executionContext,
                                                     final Map<Integer, Object> failedMap,
                                                     final List<DispatcherResult> resultList,
                                                     final List<ExecutionItem> iWorkflowCmdItems,
                                                     final boolean keepgoing) throws
        WorkflowStepFailureException {

        boolean workflowsuccess = true;
        int c = 1;
        for (final ExecutionItem cmd : iWorkflowCmdItems) {
            if (!executeWFItem(executionContext, failedMap, resultList, c, cmd, keepgoing)) {
                workflowsuccess = false;
            }
            c++;
        }
        return workflowsuccess;
    }

    /**
     * Convert list of DispatcherResult items to map of Node name to Map of InterpreterResult items keyed by index in
     * the list (0-first)
     *
     * @param resultList dispatcher result list
     *
     * @return map of node name to Map of InterpreterResult items keyed by index in the list (0-first)
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
        for (final Integer integer : failedMap.keySet()) {
            final Object o = failedMap.get(integer);
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

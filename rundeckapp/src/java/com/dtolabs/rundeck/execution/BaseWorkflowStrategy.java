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

/*
* BaseWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:19:17 PM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.context.UserContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BaseWorkflowStrategy is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public abstract class BaseWorkflowStrategy implements WorkflowStrategy {
    final WorkflowExecutionItem item;
    final ExecutionService executionService;
    final ExecutionListener listener;
    final Framework framework;

    public BaseWorkflowStrategy(WorkflowExecutionItem item, ExecutionService executionService,
                                     ExecutionListener listener, Framework framework) {
        this.item = item;
        this.executionService = executionService;
        this.listener = listener;
        this.framework = framework;
    }

    public ExecutionItem itemForWFCmdItem(final IWorkflowCmdItem cmd, final String project, final NodeSet nodeset,
                                          final String user,
                                          final int loglevel, final Map<String, Map<String, String>> dataContext) throws
        FileNotFoundException {
        final UserContext userContext = UserContext.create(user);
        if (cmd.getAdhocExecution()) {
            final IDispatchedScript script = createScriptContext(cmd, project, nodeset, loglevel, dataContext);
            return new DispatchedScriptExecutionItem() {

                public IDispatchedScript getDispatchedScript() {
                    return script;
                }

                @Override
                public String toString() {
                    return "dscript: " + getDispatchedScript();
                }
            };
        } else if (cmd instanceof IWorkflowJobItem) {
            final IWorkflowJobItem jobcmditem = (IWorkflowJobItem) cmd;
            String[] res = null;
            if (null != cmd.getArgString()) {
                final List<String> stringList = CLIUtils.splitArgLine(cmd.getArgString());
                res = stringList.toArray(new String[stringList.size()]);
            }
            final String[] args = res;
            return new JobExecutionItem() {
                public String getJobIdentifier() {
                    return jobcmditem.getJobIdentifier();
                }

                public NodeSet getNodeSet() {
                    return nodeset;
                }

                public String[] getArgs() {
                    return args;
                }

                public int getLoglevel() {
                    return loglevel;
                }

                public String getUser() {
                    return user;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return dataContext;
                }
            };
        } else {
            throw new IllegalArgumentException("Workflow command item was not valid");
        }
    }


    /**
     * Create script context
     *
     * @param cmd      workflow command item
     * @param nodeset  nodeset
     * @param loglevel loglevel
     *
     * @return script context
     */
    IDispatchedScript createScriptContext(final IWorkflowCmdItem cmd, final String project, final NodeSet nodeset,
                                          final int loglevel, final Map<String, Map<String, String>> dataContext) throws
        FileNotFoundException {
        final InputStream instream;
        if (null == cmd.getAdhocRemoteString() && null == cmd.getAdhocLocalString() && null != cmd.getAdhocFilepath()) {
            //open a file input stream for the local script file if necessary
            instream = new FileInputStream(cmd.getAdhocFilepath());
        } else {
            instream = null;
        }
        final ArrayList<String> argslist = new ArrayList<String>();
        if (null != cmd.getAdhocRemoteString()) {
            argslist.addAll(CLIUtils.splitArgLine(cmd.getAdhocRemoteString()));
        }
        if (null != cmd.getArgString()) {
            argslist.addAll(CLIUtils.splitArgLine(cmd.getArgString()));
        }

        final String[] args = argslist.toArray(new String[argslist.size()]);
        final String lscript;
        if (null != cmd.getAdhocLocalString()) {
            if (null != dataContext && dataContext.size() > 0) {
                lscript = DataContextUtils.replaceDataReferences(cmd.getAdhocLocalString(), dataContext);
            } else {
                lscript = cmd.getAdhocLocalString();
            }
        } else {
            lscript = null;
        }
        final String scptfile = cmd.getAdhocFilepath();

        return new IDispatchedScript() {

            public NodeSet getNodeSet() {
                return nodeset;
            }

            public String getFrameworkProject() {
                return null != cmd.getProject() ? cmd.getProject() : project;
            }

            public String getScript() {
                return lscript;
            }

            public InputStream getScriptAsStream() {
                return instream;
            }

            public String getServerScriptFilePath() {
                return scptfile;
            }

            public String[] getArgs() {
                return args;
            }

            public int getLoglevel() {
                return loglevel;
            }

            public Map<String, Map<String, String>> getDataContext() {
                return dataContext;
            }
        };
    }


    /**
     * Execute a workflow item, returns true if the item succeeds.  This method will throw an exception
     * if the workflow item fails and the Workflow is has keepgoing==false.
     *
     * @param item             the workflow item
     * @param executionService the execution service
     * @param listener         a listener
     * @param workflow         the workflow
     * @param failedList       List to add any messages if the item fails
     * @param resultList       List to add any Objects that are results of execution
     * @param c                index of the WF item
     * @param cmd              WF item descriptor
     * @param nodeSet          NodeSet to execute item on
     *
     * @return true if the execution succeeds, false otherwise
     *
     * @throws WorkflowAction.WorkflowStepFailureException
     *          if underlying WF item throws exception and the workflow is not "keepgoing", or the result from the
     *          execution includes an exception
     */
    protected boolean executeWFItem(final WorkflowExecutionItem item,
                                    final ExecutionService executionService,
                                    final ExecutionListener listener,
                                    final IWorkflow workflow,
                                    final List<String> failedList,
                                    final List resultList,
                                    final int c,
                                    final IWorkflowCmdItem cmd,
                                    final NodeSet nodeSet) throws WorkflowAction.WorkflowStepFailureException {
        //TODO evaluate conditionals set for cmd within the data context, and skip cmd if necessary
        listener.log(Constants.DEBUG_LEVEL, c + ": " + cmd.toString());
        ExecutionResult result = null;
        boolean itemsuccess = false;
        Throwable wfstepthrowable = null;
        try {
            final ExecutionItem newExecItem = itemForWFCmdItem(cmd, item.getProject(), nodeSet,
                item.getUser(),
                item.getLoglevel(), item.getDataContext());
            listener.log(Constants.DEBUG_LEVEL, "ExecutionItem created, executing: " + newExecItem);
            result = executionService.executeItem(newExecItem);
            itemsuccess = null != result && result.isSuccess();
        } catch (Throwable exc) {
            if (workflow.isKeepgoing()) {
                //don't fail
                listener.log(Constants.ERR_LEVEL, c + ": wf item failed: " + exc.getMessage());
                listener.log(Constants.VERBOSE_LEVEL, c + ": wf item failed: " + org.apache.tools.ant.util
                    .StringUtils.getStackTrace(exc));
                wfstepthrowable = exc;
                itemsuccess=false;
            } else {
                listener.log(Constants.ERR_LEVEL, c + ": wf item failed: " + exc.getMessage());
                throw new WorkflowAction.WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw exception: " + exc.getMessage(), exc, c);
            }
        }

        if (itemsuccess) {
            //TODO: evaluate result object and set result data into the data context
            if (null != result.getResultObject()) {
                resultList.add(result.getResultObject());
            }
            listener.log(Constants.DEBUG_LEVEL, c + ": ExecutionItem finished, result: " + result);
        } else if (workflow.isKeepgoing()) {
            //don't fail yet
            failedList.add(
                "Step " + c + " failed: " + (null != wfstepthrowable ? wfstepthrowable.getMessage()
                                                                     : (null != result && null != result
                                                                         .getException() ? result
                                                                         .getException() : (null != result
                                                                                            ? result
                                                                         .getResultObject() : "no result"))));
            listener.log(Constants.DEBUG_LEVEL, "Workflow continues");
        } else {
            if (null != result && null != result.getException()) {
                throw new WorkflowAction.WorkflowStepFailureException(
                    "Step " + c + " of the workflow threw an exception: " + result.getException().getMessage(),
                    result.getException(), c);
            } else {
                throw new WorkflowAction.WorkflowStepFailureException(
                    "Step " + c + " of the workflow failed with result: " + (result != null ? result
                        .getResultObject() : null), result, c);
            }
        }
        return itemsuccess;
    }

    protected boolean executeWorkflowItemsForNodeSet(IWorkflow workflow, List<String> failedList,
                                         List resultList, List<IWorkflowCmdItem> iWorkflowCmdItems,
                                         final NodeSet nodeSet) throws
        WorkflowAction.WorkflowStepFailureException {
        boolean workflowsuccess=true;
        int c=1;
        for (final IWorkflowCmdItem cmd : iWorkflowCmdItems) {
            if(!executeWFItem(item, executionService, listener, workflow, failedList,
                resultList, c, cmd, nodeSet)) {
                workflowsuccess = false;
            }
            c++;
        }
        return workflowsuccess;
    }
}

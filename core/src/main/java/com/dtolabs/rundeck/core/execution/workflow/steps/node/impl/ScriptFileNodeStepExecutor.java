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
* ExecFileCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:26 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;

import java.io.File;


/**
 * ExecFileCommandInterpreter uses ExecutionService to execute a script on a node.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileNodeStepExecutor implements NodeStepExecutor {
    public static final String SERVICE_IMPLEMENTATION_NAME = "script";
    public static final String SCRIPT_FILE_REMOVE_TMP = "script-step-remove-tmp-file";
    private Framework framework;

    public ScriptFileNodeStepExecutor(Framework framework) {
        this.framework = framework;
    }

    public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item, INodeEntry node)
        throws
        NodeStepException {
        final ScriptFileCommand script = (ScriptFileCommand) item;
        final ExecutionService executionService = framework.getExecutionService();
        final String filepath; //result file path
        try {
            if (null != script.getScript()) {
                filepath = executionService.fileCopyScriptContent(context, script.getScript(), node);
            } else if (null != script.getServerScriptFilePath()) {
                filepath = executionService.fileCopyFile(context, new File(
                    script.getServerScriptFilePath()), node);
            } else {
                filepath = executionService.fileCopyFileStream(context, script.getScriptAsStream(), node);
            }
        } catch (FileCopierException e) {
            throw new NodeStepException(e.getMessage(), e, e.getFailureReason(), node.getNodename());
        }

        return executeRemoteScript(context, context.getFramework(), node, script.getArgs(), filepath,
                script.getScriptInterpreter(), script.getInterpreterArgsQuoted());
    }

    /**
     * Execute a scriptfile already copied to a remote node with the given args
     *
     * @param context   context
     * @param framework framework
     * @param node      the node
     * @param args      arguments to script
     * @param filepath  the remote path for the script
     */
    public static NodeStepResult executeRemoteScript(final ExecutionContext context,
                                                     final Framework framework,
                                                     final INodeEntry node,
                                                     final String[] args,
                                                     final String filepath) throws NodeStepException {
        return executeRemoteScript(context, framework, node, args, filepath, null, false);
    }

    /**
     * Execute a scriptfile already copied to a remote node with the given args
     *
     * @param context               context
     * @param framework             framework
     * @param node                  the node
     * @param args                  arguments to script
     * @param filepath              the remote path for the script
     * @param scriptInterpreter     interpreter used to invoke the script
     * @param interpreterargsquoted if true, pass the file and script args as a single argument to the interpreter
     */
    public static NodeStepResult executeRemoteScript(final ExecutionContext context,
                                                     final Framework framework,
                                                     final INodeEntry node,
                                                     final String[] args,
                                                     final String filepath,
                                                     final String scriptInterpreter,
                                                     final boolean interpreterargsquoted) throws NodeStepException {
        boolean removeFile = true;
        if (null != node.getAttributes() && null != node.getAttributes().get(SCRIPT_FILE_REMOVE_TMP)) {
            removeFile = Boolean.parseBoolean(node.getAttributes().get(SCRIPT_FILE_REMOVE_TMP));
        }
        return executeRemoteScript(context, framework, node, args, filepath, scriptInterpreter,
                interpreterargsquoted, removeFile);
    }
    /**
     * Execute a scriptfile already copied to a remote node with the given args
     *
     * @param context               context
     * @param framework             framework
     * @param node                  the node
     * @param args                  arguments to script
     * @param filepath              the remote path for the script
     * @param scriptInterpreter     interpreter used to invoke the script
     * @param interpreterargsquoted if true, pass the file and script args as a single argument to the interpreter
     */
    public static NodeStepResult executeRemoteScript(final ExecutionContext context,
                                                     final Framework framework,
                                                     final INodeEntry node,
                                                     final String[] args,
                                                     final String filepath,
                                                     final String scriptInterpreter,
                                                     final boolean interpreterargsquoted,
                                                     final boolean removeFile) throws NodeStepException {
        /**
         * TODO: Avoid this horrific hack. Discover how to get SCP task to preserve the execute bit.
         */
        if (!"windows".equalsIgnoreCase(node.getOsFamily())) {
            //perform chmod+x for the file

            final NodeExecutorResult nodeExecutorResult = framework.getExecutionService().executeCommand(
                    context, ExecArgList.fromStrings(false, "chmod", "+x", filepath), node);
            if (!nodeExecutorResult.isSuccess()) {
                return nodeExecutorResult;
            }
        }

        //build arg list to execute the script
        ExecArgList scriptArgList = ScriptExecUtil.createScriptArgList(
                filepath,
                null,
                args,
                scriptInterpreter,
                interpreterargsquoted
        );

        NodeExecutorResult nodeExecutorResult = framework.getExecutionService().executeCommand(context,
                scriptArgList, node);

        if (removeFile) {
            //remove file
            final NodeExecutorResult nodeExecutorResult2 = framework.getExecutionService().executeCommand(
                    context, removeArgsForOsFamily(filepath, node.getOsFamily()), node);
            if (!nodeExecutorResult2.isSuccess()) {
                if (null != context.getExecutionListener()) {
                    context.getExecutionListener().log(1, "Failed to remove remote file: " + filepath);
                }
            }
        }
        return nodeExecutorResult;
    }

    /**
     * Return ExecArgList for removing a file for the given OS family
     * @param filepath
     * @param osFamily
     * @return
     */
    public static ExecArgList removeArgsForOsFamily(String filepath, String osFamily) {
        if("windows".equalsIgnoreCase(osFamily)){
            return ExecArgList.fromStrings(false, "del", filepath);
        }else{
            return ExecArgList.fromStrings(false, "rm", "-f", filepath);
        }
    }
}

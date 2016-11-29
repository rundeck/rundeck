/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.impl.common.DefaultFileCopierUtil;
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;

import java.io.File;
import java.io.InputStream;

/**
 * Created by greg on 7/15/16.
 */
public class DefaultScriptFileNodeStepUtils implements ScriptFileNodeStepUtils {
    public static final String SCRIPT_FILE_REMOVE_TMP = "script-step-remove-tmp-file";

    private FileCopierUtil fileCopierUtil = new DefaultFileCopierUtil();

    /**
     * Execute a script on a remote node
     *
     * @param context              context
     * @param node                 node
     * @param scriptString         string
     * @param serverScriptFilePath file
     * @param scriptAsStream       stream
     * @param fileExtension        file extension
     * @param args                 script args
     * @param scriptInterpreter    invoker string
     * @param quoted               true if args are quoted
     * @param executionService     service
     *
     * @return execution result
     *
     * @throws NodeStepException on error
     */
    @Override
    public NodeStepResult executeScriptFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            String fileExtension,
            String[] args,
            String scriptInterpreter,
            boolean quoted,
            final ExecutionService executionService,
            final boolean expandTokens
    ) throws NodeStepException
    {
        final String filename;

        if (null != scriptString) {
            filename = "dispatch-script.tmp";
        } else if (null != serverScriptFilePath) {
            filename = new File(serverScriptFilePath).getName();
        } else {
            filename = "dispatch-script.tmp";
        }
        String ident = null != context.getDataContext() && null != context.getDataContext().get("job")
                       ? context.getDataContext().get("job").get("execid")
                       : null;
        String filepath = fileCopierUtil.generateRemoteFilepathForNode(
                node,
                context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                context.getFramework(),
                filename,
                fileExtension,
                ident
        );
        try {
            File temp = writeScriptToTempFile(
                    context,
                    node,
                    scriptString,
                    serverScriptFilePath,
                    scriptAsStream,
                    expandTokens
            );
            try {
                filepath = executionService.fileCopyFile(
                        context,
                        temp,
                        node,
                        filepath
                );
            } finally {
                //clean up
                ScriptfileUtils.releaseTempFile(temp);
            }
        } catch (FileCopierException e) {
            throw new NodeStepException(
                    e.getMessage(),
                    e,
                    e.getFailureReason(),
                    node.getNodename()
            );
        }

        return executeRemoteScript(
                context,
                context.getFramework(),
                node,
                args,
                filepath,
                scriptInterpreter,
                quoted
        );
    }

    /**
     * Copy the script input to a temp file and expand embedded tokens,
     * if it is a string or inputstream.  If it is a local file,
     * use the original without modification
     *
     * @param context              context
     * @param node                 node
     * @param scriptString         string
     * @param serverScriptFilePath file
     * @param scriptAsStream       stream
     *
     * @return temp file
     *
     * @throws FileCopierException on error
     */
    @Override
    public File writeScriptToTempFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            boolean expandTokens
    ) throws FileCopierException
    {
        File temp;
        if (null != scriptString) {
            //expand tokens in the script
            temp = fileCopierUtil.writeScriptTempFile(
                    context,
                    null,
                    null,
                    scriptString,
                    node,
                    expandTokens
            );
        } else if (null != serverScriptFilePath) {
            //DON'T expand tokens in the script
            //TODO: make token expansion optional for local file sources
            temp = new File(serverScriptFilePath);
        } else {
            //expand tokens in the script
            temp = fileCopierUtil.writeScriptTempFile(
                    context,
                    null,
                    scriptAsStream,
                    null,
                    node,
                    expandTokens
            );
        }
        return temp;
    }

    /**
     * Execute a scriptfile already copied to a remote node with the given args
     *
     * @param context   context
     * @param framework framework
     * @param node      the node
     * @param args      arguments to script
     * @param filepath  the remote path for the script
     *
     * @return the result
     *
     * @throws NodeStepException on error
     */
    @Override
    public NodeStepResult executeRemoteScript(
            final ExecutionContext context,
            final Framework framework,
            final INodeEntry node,
            final String[] args,
            final String filepath
    ) throws NodeStepException
    {
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
     *
     * @return result
     *
     * @throws NodeStepException on error
     */
    @Override
    public NodeStepResult executeRemoteScript(
            final ExecutionContext context,
            final Framework framework,
            final INodeEntry node,
            final String[] args,
            final String filepath,
            final String scriptInterpreter,
            final boolean interpreterargsquoted
    ) throws NodeStepException
    {
        boolean removeFile = true;
        if (null != node.getAttributes() && null != node.getAttributes().get(SCRIPT_FILE_REMOVE_TMP)) {
            removeFile = Boolean.parseBoolean(node.getAttributes().get(SCRIPT_FILE_REMOVE_TMP));
        }
        return executeRemoteScript(context, framework, node, args, filepath, scriptInterpreter,
                                   interpreterargsquoted, removeFile
        );
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
     * @param removeFile            if true, remove the file after execution
     *
     * @return result
     *
     * @throws NodeStepException on error
     */
    @Override
    public NodeStepResult executeRemoteScript(
            final ExecutionContext context,
            final Framework framework,
            final INodeEntry node,
            final String[] args,
            final String filepath,
            final String scriptInterpreter,
            final boolean interpreterargsquoted,
            final boolean removeFile
    ) throws NodeStepException
    {
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
                                                                                               scriptArgList, node
        );

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
     *
     * @param filepath path
     * @param osFamily family
     *
     * @return arg list
     */
    @Override
    public ExecArgList removeArgsForOsFamily(String filepath, String osFamily) {
        if ("windows".equalsIgnoreCase(osFamily)) {
            return ExecArgList.fromStrings(false, "del", filepath);
        } else {
            return ExecArgList.fromStrings(false, "rm", "-f", filepath);
        }
    }

    public FileCopierUtil getFileCopierUtil() {
        return fileCopierUtil;
    }

    public void setFileCopierUtil(FileCopierUtil fileCopierUtil) {
        this.fileCopierUtil = fileCopierUtil;
    }
}

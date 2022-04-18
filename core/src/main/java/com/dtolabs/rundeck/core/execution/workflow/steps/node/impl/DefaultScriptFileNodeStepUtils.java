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
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.impl.common.DefaultFileCopierUtil;
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 7/15/16.
 */
public class DefaultScriptFileNodeStepUtils implements ScriptFileNodeStepUtils {
    public static final Logger logger = LoggerFactory.getLogger(DefaultScriptFileNodeStepUtils.class.getName());

    public static final String SCRIPT_FILE_REMOVE_TMP = "script-step-remove-tmp-file";
    public static final String MESSAGE_ERROR_FILE_BUSY_PATTERN = "Cannot run program.+: error=26.*";
    public static final String NODE_ATTR_FILE_BUSY_ERR_RETRY = "file-busy-err-retry";
    public static final String NODE_ATTR_ENABLE_SYNC_COMMAND = "enable-sync";
    private static final int MAX_TIME_TO_WAIT_BEFORE_TRY_AGAIN = 3000;

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
            final NodeExecutionService executionService,
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
        } catch (ExecutionException e) {
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
            // if the filepath has option tokens, they will be expanded.
            File serverScriptFile;
            if( DataContextUtils.hasOptionsInString(serverScriptFilePath) ){
                Map<String, Map<String, String>> optionsContext = new HashMap();
                optionsContext.put("option", context.getDataContext().get("option"));
                String expandedVarsInURL = DataContextUtils.replaceDataReferencesInString(serverScriptFilePath, optionsContext);
                serverScriptFile = new File(expandedVarsInURL);
            }else{
                serverScriptFile = new File(serverScriptFilePath);
            }
            if(expandTokens){
                try(InputStream inputStream = new FileInputStream(serverScriptFile)) {
                    serverScriptFile = fileCopierUtil.writeScriptTempFile(
                            context,
                            null,
                            inputStream,
                            null,
                            node,
                            expandTokens
                    );
                } catch (IOException e) {
                    throw new FileCopierException(
                            "error writing script to tempfile: " + e.getMessage(),
                            StepFailureReason.IOFailure, e);
                }
            }

            temp = serverScriptFile;
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
        boolean retryExecuteCommand = false; //retry if chmod command continues to lock the file the moment it is executed
        if (!"windows".equalsIgnoreCase(node.getOsFamily())) {
            //perform chmod+x for the file
            boolean featureQuotingBackwardCompatible = Boolean.valueOf(context.getIFramework().getPropertyRetriever().getProperty("rundeck.feature.quoting.backwardCompatible"));
            final NodeExecutorResult nodeExecutorResult = framework.getExecutionService().executeCommand(
                    context, ExecArgList.fromStrings(featureQuotingBackwardCompatible, false, "chmod", "+x", filepath), node);

            if (!nodeExecutorResult.isSuccess()) {
                return nodeExecutorResult;
            }

            Map<String, String> nodeAttribute = node.getAttributes();
            if(BooleanUtils.toBoolean(nodeAttribute.get(NODE_ATTR_ENABLE_SYNC_COMMAND))) {
                //perform sync to prevent the file from being busy when running
                final NodeExecutorResult nodeExecutorSyncResult = framework.getExecutionService().executeCommand(
                        context, ExecArgList.fromStrings(featureQuotingBackwardCompatible , false, "sync"), node);

                if (!nodeExecutorSyncResult.isSuccess()) {
                    return nodeExecutorSyncResult;
                }
            }

            retryExecuteCommand = BooleanUtils.toBoolean(nodeAttribute.get(NODE_ATTR_FILE_BUSY_ERR_RETRY));
        }

        //build arg list to execute the script
        ExecArgList scriptArgList = ScriptExecUtil.createScriptArgList(
                filepath,
                null,
                args,
                scriptInterpreter,
                interpreterargsquoted
        );

       NodeExecutorResult nodeExecutorResult = executeCommand(framework, context,
                scriptArgList, node, retryExecuteCommand, 500);

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

    private NodeExecutorResult executeCommand(final Framework framework, final ExecutionContext context, final ExecArgList scriptArgList, final INodeEntry node, boolean retryAttempt, int timeToWait){

        NodeExecutorResult nodeExecutorResult = framework.getExecutionService().executeCommand(context,
                scriptArgList, node
        );

        boolean isFileBusy = checkIfFileBusy(nodeExecutorResult);
        if(retryAttempt && isFileBusy){
            context.getExecutionLogger().log(
                    5,
                    "File is busy. Retrying..."
            );
            return attemptExecuteCommand(framework, context, scriptArgList, node, timeToWait);
        } else if(isFileBusy) {
            return NodeExecutorResultImpl.createFailure(
                    nodeExecutorResult.getFailureReason(),
                    nodeExecutorResult.getFailureMessage() + " Set node attribute 'file-busy-err-retry=true' to enable retrying",
                    node
            );
        } else {
            return nodeExecutorResult;
        }
    }

    private NodeExecutorResult attemptExecuteCommand(final Framework framework, final ExecutionContext context, final ExecArgList scriptArgList, final INodeEntry node, int timeToWait){
        timeToWait = timeToWait + 500; //ms
        boolean retryAttempt = timeToWait < MAX_TIME_TO_WAIT_BEFORE_TRY_AGAIN;
        context.getExecutionLogger().log(
                5,
                "Waiting " + (timeToWait / 1000) + " seconds before try again"
        );
        try {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e) {
            logger.error("InterruptedException: " + e);
        }
        return executeCommand(framework, context, scriptArgList, node, retryAttempt, timeToWait);
    }

    private boolean checkIfFileBusy(NodeExecutorResult nodeExecutorResult){
        String failureMessage = nodeExecutorResult.getFailureMessage();
        return null != failureMessage && failureMessage.matches(MESSAGE_ERROR_FILE_BUSY_PATTERN);
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
            return ExecArgList.fromStrings(false, false, "del", filepath);
        } else {
            return ExecArgList.fromStrings(false, false, "rm", "-f", filepath);
        }
    }

    public FileCopierUtil getFileCopierUtil() {
        return fileCopierUtil;
    }

    public void setFileCopierUtil(FileCopierUtil fileCopierUtil) {
        this.fileCopierUtil = fileCopierUtil;
    }
}

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

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.NodeExecutionService;
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.io.File;
import java.io.InputStream;

/**
 * Created by greg on 7/15/16.
 */
public interface ScriptFileNodeStepUtils {
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
    NodeStepResult executeScriptFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            String fileExtension,
            String[] args,
            String scriptInterpreter,
            boolean quoted,
            NodeExecutionService executionService,
            boolean expandTokens
    ) throws NodeStepException;

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
     * @param inputStream          inputstream to send to the script
     * @param quoted               true if args are quoted
     * @param executionService     service
     * @return execution result
     * @throws NodeStepException on error
     */
    NodeStepResult executeScriptFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            String fileExtension,
            String[] args,
            String scriptInterpreter,
            InputStream inputStream,
            boolean quoted,
            NodeExecutionService executionService,
            boolean expandTokens,
            FileCopierUtil.ContentModifier modifier
    ) throws NodeStepException;

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
    File writeScriptToTempFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            boolean expandTokens
    ) throws FileCopierException;

    /**
     * Copy the script input to a temp file and expand embedded tokens, if it is a string or inputstream.  If it is a
     * local file, use the original without modification
     *
     * @param context              context
     * @param node                 node
     * @param scriptString         string
     * @param serverScriptFilePath file
     * @param scriptAsStream       stream
     * @return temp file
     * @throws FileCopierException on error
     */
    File writeScriptToTempFile(
            StepExecutionContext context,
            INodeEntry node,
            String scriptString,
            String serverScriptFilePath,
            InputStream scriptAsStream,
            boolean expandTokens,
            FileCopierUtil.ContentModifier scriptModifierUtil
    ) throws FileCopierException;

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
    NodeStepResult executeRemoteScript(
            ExecutionContext context,
            IFramework framework,
            INodeEntry node,
            String[] args,
            String filepath
    ) throws NodeStepException;

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
    NodeStepResult executeRemoteScript(
            ExecutionContext context,
            IFramework framework,
            INodeEntry node,
            String[] args,
            String filepath,
            String scriptInterpreter,
            boolean interpreterargsquoted
    ) throws NodeStepException;

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
     * @param inputStream           input to send to the script
     * @return result
     * @throws NodeStepException on error
     */
    NodeStepResult executeRemoteScript(
            ExecutionContext context,
            IFramework framework,
            INodeEntry node,
            String[] args,
            String filepath,
            String scriptInterpreter,
            boolean interpreterargsquoted,
            InputStream inputStream
    ) throws NodeStepException;

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
    NodeStepResult executeRemoteScript(
            ExecutionContext context,
            IFramework framework,
            INodeEntry node,
            String[] args,
            String filepath,
            String scriptInterpreter,
            boolean interpreterargsquoted,
            boolean removeFile
    ) throws NodeStepException;

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
     * @param inputStream           input to send to the script
     * @return result
     * @throws NodeStepException on error
     */
    NodeStepResult executeRemoteScript(
            ExecutionContext context,
            IFramework framework,
            INodeEntry node,
            String[] args,
            String filepath,
            String scriptInterpreter,
            boolean interpreterargsquoted,
            boolean removeFile,
            InputStream inputStream
    ) throws NodeStepException;

    /**
     * Return ExecArgList for removing a file for the given OS family
     *
     * @param filepath path
     * @param osFamily family
     *
     * @return arg list
     */
    ExecArgList removeArgsForOsFamily(String filepath, String osFamily);
}

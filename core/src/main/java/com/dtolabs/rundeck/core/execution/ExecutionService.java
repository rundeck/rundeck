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
* ExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:07:14 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;

/**
 * ExecutionService provides interface to all dispatcher and command execution services.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ExecutionService extends FrameworkSupportService {
    public static final String SERVICE_NAME = "ExecutionService";
    /**
     * Execute the item for the given context and return the result.
     *
     *
     * @param context context
     * @param item item
     *
     * @return result
     * @deprecated use {@link #executeStep(com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext, StepExecutionItem)}
     * @throws com.dtolabs.rundeck.core.execution.service.ExecutionServiceException on error
     * @throws ExecutionException on execution error
     */
    public ExecutionResult executeItem(StepExecutionContext context, StepExecutionItem item)
        throws ExecutionException, ExecutionServiceException;

    /**
     * Execute a workflow step item for the given context and return the result.
     *
     *
     * @param context context
     * @param item item
     *
     * @return not-null result
     * @throws StepException on error
     */
    public StepExecutionResult executeStep(StepExecutionContext context, StepExecutionItem item) throws StepException;

    /**
     * Interpret the execution item within the context for the given node.
     * @param context context
     * @param item step item
     * @param node node
     * @return result
     * @throws NodeStepException on error
     */
    public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item, INodeEntry node) throws
                                                                                                          NodeStepException;


    /**
     * Dispatch the command (execution item) to all the nodes within the context.
     * @param context context
     * @param item step item
     * @return result
     * @throws DispatcherException on dispatch error
     * @throws ExecutionServiceException on service error
     */
    public DispatcherResult dispatchToNodes(StepExecutionContext context, NodeStepExecutionItem item)
        throws DispatcherException, ExecutionServiceException;
    /**
     * Dispatch the command (execution item) to all the nodes within the context.
     * @param context context
     * @param item step item
     * @return result
     * @throws DispatcherException on dispatch error
     * @throws ExecutionServiceException on service error
     */
    public DispatcherResult dispatchToNodes(StepExecutionContext context, Dispatchable item)
        throws DispatcherException, ExecutionServiceException;


    /**
     * Copy inputstream as a file to the node.
     * @param context context
     * @param input input stream
     * @param node node
     * @throws FileCopierException on error
     * @return filepath on the node for the destination file.
     * @deprecated use {@link #fileCopyFileStream(ExecutionContext, java.io.InputStream, com.dtolabs.rundeck.core.common.INodeEntry, String)}
     */
    public String fileCopyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException;

    /**
     * Copy inputstream as a file to the node to a specific path

     * @param context context
     * @param input input stream
     * @param node node
     * @param destinationPath destination path
     * @throws FileCopierException on error
     * @return filepath on the node for the destination file.
     */
    public String fileCopyFileStream(final ExecutionContext context, InputStream input, INodeEntry node,
            String destinationPath) throws
            FileCopierException;

    /**
     * Copy file to the node as a script file to the temp file location.
     *
     * @param context context
     * @param file input file
     * @param node node
     * @throws FileCopierException on error
     * @return filepath for the copied file on the node.
     * @deprecated use {@link #fileCopyFile(ExecutionContext, java.io.File, com.dtolabs.rundeck.core.common.INodeEntry, String)}
     */
    public String fileCopyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

    /**
     * Copy file to the node to a specific path

     * @param context context
     * @param file input file
     * @param node node
     * @param destinationPath destination path
     * @throws FileCopierException on error
     * @return filepath
     */
    public String fileCopyFile(final ExecutionContext context, File file, INodeEntry node,
            String destinationPath) throws FileCopierException;

    /**
     * Copy string as a file to the node,
     *
     * @param context context
     * @param script script string
     * @param node node
     * @throws FileCopierException on error
     * @return filepath for the copied file on the node
     *
     * @deprecated use {@link #fileCopyScriptContent(ExecutionContext, String, com.dtolabs.rundeck.core.common.INodeEntry, String)}
     */
    public String fileCopyScriptContent(final ExecutionContext context, String script,
                                        INodeEntry node) throws
        FileCopierException;
    /**
     * Copy string as a file to the node to a specific path
     *
     * @param context context
     * @param script script string
     * @param node node
     * @param destinationPath destination path
     * @throws FileCopierException on error
     * @return filepath for the copied file on the node
     */
    public String fileCopyScriptContent(final ExecutionContext context, String script,
                                        INodeEntry node, String destinationPath) throws
        FileCopierException;

    /**
     * Execute a command within the context on the node.
     * @param context context
     * @param command command strings
     * @param node node
     * @return result
     * @deprecated use {@link #executeCommand(ExecutionContext, ExecArgList, com.dtolabs.rundeck.core.common.INodeEntry)}
     *
     */
    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node);

    /**
     * Execute a command within the context on the node.
     * @param context context
     * @param command command
     * @param node node
     * @return result
     */
    public NodeExecutorResult executeCommand(ExecutionContext context, ExecArgList command, INodeEntry node) ;
}

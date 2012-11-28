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
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
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
     * @param item item
     *
     * @return result
     * @deprecated use {@link #executeStep(com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext, StepExecutionItem)}
     */
    public ExecutionResult executeItem(StepExecutionContext context, StepExecutionItem item) throws ExecutionException;

    /**
     * Execute a workflow step item for the given context and return the result.
     *
     *
     *
     * @param item item
     *
     * @return result
     */
    public StepExecutionResult executeStep(StepExecutionContext context, StepExecutionItem item) throws ExecutionException;

    /**
     * Interpret the execution item within the context for the given node.
     */
    public NodeStepResult executeNodeStep(StepExecutionContext context, NodeStepExecutionItem item, INodeEntry node) throws
                                                                                                          NodeStepException;


    /**
     * Dispatch the command (execution item) to all the nodes within the context.
     */
    public DispatcherResult dispatchToNodes(StepExecutionContext context, NodeStepExecutionItem item) throws DispatcherException;
    /**
     * Dispatch the command (execution item) to all the nodes within the context.
     */
    public DispatcherResult dispatchToNodes(StepExecutionContext context, Dispatchable item) throws DispatcherException;


    /**
     * Copy inputstream as a file to the node.
     *
     * @return filepath on the node for the destination file.
     */
    public String fileCopyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException;

    /**
     * Copy file to the node.
     *
     * @return filepath for the copied file on the node.
     */
    public String fileCopyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

    /**
     * Copy string as a file to the node,
     *
     * @return filepath for the copied file on the node
     */
    public String fileCopyScriptContent(final ExecutionContext context, String script,
                                        INodeEntry node) throws
        FileCopierException;

    /**
     * Execute a command within the context on the node.
     */
    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) throws
        ExecutionException;
}

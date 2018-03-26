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

/*
* NewExecutionServiceImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 11:19 AM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener;
import com.dtolabs.rundeck.core.execution.workflow.steps.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.*;
import com.dtolabs.rundeck.core.logging.PluginLoggingManager;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * NewExecutionServiceImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ExecutionServiceImpl implements ExecutionService {
    private final Framework framework;

    public ExecutionServiceImpl(Framework framework) {

        this.framework = framework;
    }

    protected WorkflowExecutionListener getWorkflowListener(final ExecutionContext executionContext) {
        WorkflowExecutionListener wlistener = executionContext.getWorkflowExecutionListener();
        if(null!=wlistener){
            return wlistener;
        }
        final ExecutionListener elistener = executionContext.getExecutionListener();
        if (null != elistener && elistener instanceof WorkflowExecutionListener) {
            return (WorkflowExecutionListener) elistener;
        }
        return null;
    }


    public StepExecutionResult executeStep(StepExecutionContext context, StepExecutionItem item) throws StepException {

        final StepExecutor executor;
        try {
            executor = framework.getStepExecutionService().getExecutorForItem(item);
        } catch (ExecutionServiceException e) {
            return new StepExecutionResultImpl(e, ServiceFailureReason.ServiceFailure, e.getMessage());
        }

        StepExecutionResult result = null;
        try {
            if (null != getWorkflowListener(context)) {
                getWorkflowListener(context).beginStepExecution(executor, context, item);
            }

            PluginLoggingManager pluginLogging = null;
            if (null != context.getLoggingManager()) {
                pluginLogging = context
                        .getLoggingManager().createPluginLogging(context, item);
            }
            if (null != pluginLogging) {
                pluginLogging.begin();
            }
            try {
                context
                    .getPluginControlService()
                    .checkDisabledPlugin(
                        item.getType(),
                        ServiceNameConstants.WorkflowStep
                    );
                result = executor.executeWorkflowStep(context, item);
            } finally {
                if (null != pluginLogging) {
                    pluginLogging.end();
                }
            }
        } finally {
            if (null != getWorkflowListener(context)) {
                getWorkflowListener(context).finishStepExecution(executor,result, context, item);
            }
        }
        return result;
    }

    static enum ServiceFailureReason implements FailureReason{
        ServiceFailure
    }

    public NodeStepResult executeNodeStep(StepExecutionContext context,
                                          NodeStepExecutionItem item, INodeEntry node) throws NodeStepException {

        final NodeStepExecutor interpreter;
        try {
            interpreter = framework.getNodeStepExecutorForItem(item);
        } catch (ExecutionServiceException e) {
            throw new NodeStepException(e, ServiceFailureReason.ServiceFailure, node.getNodename());
        }

        if (null != getWorkflowListener(context)) {
            getWorkflowListener(context).beginExecuteNodeStep(context, item, node);
        }
        //create node context for node and substitute data references in command

        NodeStepResult result = null;
        try {
            final ExecutionContextImpl nodeContext = new ExecutionContextImpl.Builder(context)
                    .singleNodeContext(node, true)
                    .build();

            PluginLoggingManager pluginLogging = null;
            if (null != context.getLoggingManager()) {
                pluginLogging = context
                        .getLoggingManager().createPluginLogging(nodeContext, item);
            }
            if (null != pluginLogging) {
                pluginLogging.begin();
            }
            try {
                context
                    .getPluginControlService()
                    .checkDisabledPlugin(
                        item.getNodeStepType(),
                        ServiceNameConstants.WorkflowNodeStep
                    );
                result = interpreter.executeNodeStep(nodeContext, item, node);
            } finally {
                if (null != pluginLogging) {
                    pluginLogging.end();
                }
            }
            if (!result.isSuccess()) {
                context.getExecutionListener().log(0, "Failed: " + result.toString());
            }
        } catch (NodeStepException e) {
            result = new NodeStepResultImpl(e, e.getFailureReason(), e.getMessage(), node);
            throw e;
        }catch (Throwable t) {
            result = new NodeStepResultImpl(t, StepFailureReason.Unknown, t.getMessage(), node);
            throw new NodeStepException(t, StepFailureReason.Unknown, node.getNodename());
        } finally {
            if (null != getWorkflowListener(context)) {
                getWorkflowListener(context).finishExecuteNodeStep(result, context, item, node);
            }
        }
        return result;
    }

    public DispatcherResult dispatchToNodes(StepExecutionContext context, NodeStepExecutionItem item) throws
                                                                                                      DispatcherException,
                                                                                                      ExecutionServiceException {

        return dispatchToNodesWith(context, null, item);
    }

    public DispatcherResult dispatchToNodes(StepExecutionContext context, Dispatchable item) throws
                                                                                             DispatcherException,
                                                                                             ExecutionServiceException {

        return dispatchToNodesWith(context, item, null);
    }

    private DispatcherResult dispatchToNodesWith(
            StepExecutionContext context,
            Dispatchable dispatchable,
            NodeStepExecutionItem item
    ) throws
            DispatcherException,
            ExecutionServiceException
    {
        if (null != context.getExecutionListener()) {
            if (null != item) {
                context.getExecutionListener().beginNodeDispatch(context, item);
            } else if (null != dispatchable) {
                context.getExecutionListener().beginNodeDispatch(context, dispatchable);
            }
        }
        final NodeDispatcher dispatcher = framework.getNodeDispatcherForContext(context);
        DispatcherResult result = null;
        try {
            if (null != item) {
                result = dispatcher.dispatch(context, item);
            } else {
                result = dispatcher.dispatch(context, dispatchable);
            }
        } finally {
            if (null != context.getExecutionListener()) {
                if (null != item) {
                    context.getExecutionListener().finishNodeDispatch(result, context, item);
                } else if (null != dispatchable) {
                    context.getExecutionListener().finishNodeDispatch(result, context, dispatchable);
                }
            }
        }
        return result;
    }


    public String fileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node,
            String destinationPath) throws
            FileCopierException {


        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyFileStream(context, input, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e.getMessage(), ServiceFailureReason.ServiceFailure, e);
        }
        String result = null;
        try {
            result = copier.copyFileStream(context, input, node, destinationPath);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }


    public String fileCopyFile(ExecutionContext context, File file, INodeEntry node,
            String destinationPath) throws FileCopierException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyFile(context, file, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e.getMessage(), ServiceFailureReason.ServiceFailure, e);
        }
        String result = null;
        try {
            result = copier.copyFile(context, file, node, destinationPath);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public String[] fileCopyFiles(
            ExecutionContext context,
            File basedir,
            List<File> files,
            String remotePath,
            INodeEntry node
    )
            throws FileCopierException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyFile(context, files, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e.getMessage(), ServiceFailureReason.ServiceFailure, e);
        }
        String[] result = null;
        try {
            if (copier instanceof MultiFileCopier) {
                MultiFileCopier dcopier = (MultiFileCopier) copier;
                result = dcopier.copyFiles(context, basedir, files, remotePath, node);
            }else{
                result = MultiFileCopierUtil.copyMultipleFiles(copier, context, basedir, files, remotePath, node);
            }
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishMultiFileCopy(result, context, node);
            }
        }
        return result;
    }


    public String fileCopyScriptContent(ExecutionContext context, String script, INodeEntry node, String
            destinationPath) throws FileCopierException {
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyScriptContent(context, script, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e.getMessage(), ServiceFailureReason.ServiceFailure, e);
        }
        String result = null;
        try {
            result = copier.copyScriptContent(context, script, node, destinationPath);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node) {
        return executeCommand(context, ExecArgList.fromStrings(DataContextUtils
                .stringContainsPropertyReferencePredicate, command), node);
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final ExecArgList command,
                                             final INodeEntry node) {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeExecution(context, command.asFlatStringArray(), node);
        }
        final NodeExecutor nodeExecutor;
        try {
            nodeExecutor = framework.getNodeExecutorForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new CoreException(e);
        }

        //create node context for node and substitute data references in command
        final ExecutionContextImpl nodeContext = new ExecutionContextImpl.Builder(context)
                .nodeContextData(node)
                .build();

        final ArrayList<String> commandList = command.buildCommandForNode(
                nodeContext.getSharedDataContext(),
                node.getNodename(),
                node.getOsFamily()
        );

        NodeExecutorResult result = null;
        String[] commandArray = commandList.toArray(new String[commandList.size()]);
        try {
            result = nodeExecutor.executeCommand(nodeContext, commandArray, node);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishNodeExecution(result, context, commandArray, node);
            }
        }
        return result;
    }

    public String getName() {
        return SERVICE_NAME;
    }


}

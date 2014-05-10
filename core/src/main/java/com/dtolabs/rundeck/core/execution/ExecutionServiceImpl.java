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
* NewExecutionServiceImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 11:19 AM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.cli.ExecTool;
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
import com.dtolabs.rundeck.core.utils.*;
import org.apache.commons.collections.Predicate;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        WorkflowExecutionListener wlistener = null;
        final ExecutionListener elistener = executionContext.getExecutionListener();
        if (null != elistener && elistener instanceof WorkflowExecutionListener) {
            wlistener = (WorkflowExecutionListener) elistener;
        }
        return wlistener;
    }
    public ExecutionResult executeItem(StepExecutionContext context, StepExecutionItem executionItem)
        throws ExecutionException, ExecutionServiceException {
        if (null != getWorkflowListener(context)) {
            getWorkflowListener(context).beginStepExecution(null,context, executionItem);
        }
        if (!(executionItem instanceof NodeStepExecutionItem)) {
            throw new IllegalArgumentException("Cannot dispatch item which is not a NodeStepExecutionItem: " +
                    executionItem);
        }
        NodeStepExecutionItem item = (NodeStepExecutionItem) executionItem;

        boolean success = false;
        DispatcherResult result = null;
        BaseExecutionResult baseExecutionResult = null;
        final LogReformatter formatter = createLogReformatter(null, context.getExecutionListener());
        final ThreadStreamFormatter loggingReformatter = new ThreadStreamFormatter(formatter).invoke();
        try {
            result = dispatchToNodes(context, item);
            success = result.isSuccess();
            baseExecutionResult = new BaseExecutionResult(result, success, null);
        } catch (DispatcherException e) {
            baseExecutionResult = new BaseExecutionResult(result, success, e);
        } finally {
            loggingReformatter.resetOutputStreams();
            if (null != getWorkflowListener(context)) {
                getWorkflowListener(context).finishStepExecution(null,baseExecutionResult, context, item);
            }
        }

        return baseExecutionResult;
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
            result = executor.executeWorkflowStep(context, item);
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
            result = interpreter.executeNodeStep(nodeContext, item, node);
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

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeDispatch(context, item);
        }
        final NodeDispatcher dispatcher = framework.getNodeDispatcherForContext(context);
        DispatcherResult result = null;
        try {
            result = dispatcher.dispatch(context, item);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishNodeDispatch(result, context, item);
            }
        }
        return result;
    }

    public DispatcherResult dispatchToNodes(StepExecutionContext context, Dispatchable item) throws
                                                                                             DispatcherException,
                                                                                             ExecutionServiceException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeDispatch(context, item);
        }
        final NodeDispatcher dispatcher = framework.getNodeDispatcherForContext(context);
        DispatcherResult result = null;
        try {
            result = dispatcher.dispatch(context, item);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishNodeDispatch(result, context, item);
            }
        }
        return result;
    }

    public String fileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) throws
        FileCopierException {
        return fileCopyFileStream(context, input, node, null);
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
            if (null != destinationPath && copier instanceof DestinationFileCopier) {
                DestinationFileCopier dcopier = (DestinationFileCopier) copier;
                result = dcopier.copyFileStream(context, input, node, destinationPath);
            } else {
                result = copier.copyFileStream(context, input, node);
            }
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public String fileCopyFile(ExecutionContext context, File file,
                               INodeEntry node) throws FileCopierException {
        return fileCopyFile(context, file, node, null);
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
            if (null != destinationPath && copier instanceof DestinationFileCopier) {
                DestinationFileCopier dcopier = (DestinationFileCopier) copier;
                result = dcopier.copyFile(context, file, node, destinationPath);
            }else{
                result = copier.copyFile(context, file, node);
            }
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public String fileCopyScriptContent(ExecutionContext context, String script,
            INodeEntry node) throws FileCopierException {
        return fileCopyScriptContent(context,script,node,null);
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
            if (null != destinationPath && copier instanceof DestinationFileCopier) {
                DestinationFileCopier dcopier = (DestinationFileCopier) copier;
                result = dcopier.copyScriptContent(context, script, node, destinationPath);
            }else{
                result = copier.copyScriptContent(context, script, node);
            }
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
        final ExecutionContextImpl nodeContext = new ExecutionContextImpl.Builder(context).nodeContextData(node).build();

        final ArrayList<String> commandList = command.buildCommandForNode(nodeContext.getDataContext(),
                node.getOsFamily());

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

    /**
     * @deprecated
     */
    private static class ContextLoggerExecutionListenerMapGenerator implements MapGenerator<String,String>{
        final ContextLoggerExecutionListener ctxListener;

        private ContextLoggerExecutionListenerMapGenerator(final ContextLoggerExecutionListener ctxListener) {
            this.ctxListener = ctxListener;
        }

        public Map<String, String> getMap() {
            return ctxListener.getLoggingContext();
        }
    }
    /**
     * Create a LogReformatter for the specified node and listener. If the listener is a {@link ContextLoggerExecutionListener},
     * then the context map data is used by the reformatter.
     * @deprecated
     */
    public static LogReformatter createLogReformatter(final INodeEntry node, final ExecutionListener listener) {
        LogReformatter gen;
        if (null != listener && listener.isTerse()) {
            gen = null;
        } else {
            String logformat = ExecTool.DEFAULT_LOG_FORMAT;
            if (null != listener && null != listener.getLogFormat()) {
                logformat = listener.getLogFormat();
            }
            if (listener instanceof ContextLoggerExecutionListener) {
                final ContextLoggerExecutionListener ctxListener = (ContextLoggerExecutionListener) listener;
                gen = new LogReformatter(logformat, new ContextLoggerExecutionListenerMapGenerator(ctxListener));
            } else {
                final HashMap<String, String> baseContext = new HashMap<String, String>();
                //discover node name and username
                baseContext.put("node", null != node?node.getNodename():"");
                baseContext.put("user", null != node?node.extractUserName():"");
                gen = new LogReformatter(logformat, baseContext);
            }

        }
        return gen;
    }

    /**
     * @deprecated
     */
    static class ThreadStreamFormatter {
        final LogReformatter gen;
        private ThreadBoundOutputStream threadBoundSysOut;
        private ThreadBoundOutputStream threadBoundSysErr;
        private OutputStream origout;
        private OutputStream origerr;

        ThreadStreamFormatter(final LogReformatter gen) {
            this.gen = gen;
        }

        public ThreadBoundOutputStream getThreadBoundSysOut() {
            return threadBoundSysOut;
        }

        public ThreadBoundOutputStream getThreadBoundSysErr() {
            return threadBoundSysErr;
        }

        public OutputStream getOrigout() {
            return origout;
        }

        public OutputStream getOrigerr() {
            return origerr;
        }


        public ThreadStreamFormatter invoke() {
            //bind System printstreams to the thread
            threadBoundSysOut = ThreadBoundOutputStream.bindSystemOut();
            threadBoundSysErr = ThreadBoundOutputStream.bindSystemErr();

            //get outputstream for reformatting destination
            origout = threadBoundSysOut.getThreadStream();
            origerr = threadBoundSysErr.getThreadStream();

            //replace any existing logreformatter
            final FormattedOutputStream outformat;
            if (origout instanceof FormattedOutputStream) {
                final OutputStream origsink = ((FormattedOutputStream) origout).getOriginalSink();
                outformat = new FormattedOutputStream(gen, origsink);
            } else {
                outformat = new FormattedOutputStream(gen, origout);
            }
            outformat.setContext("level", "INFO");

            final FormattedOutputStream errformat;
            if (origerr instanceof FormattedOutputStream) {
                final OutputStream origsink = ((FormattedOutputStream) origerr).getOriginalSink();
                errformat = new FormattedOutputStream(gen, origsink);
            } else {
                errformat = new FormattedOutputStream(gen, origerr);
            }
            errformat.setContext("level", "ERROR");

            //install the OutputStreams for the thread
            threadBoundSysOut.installThreadStream(outformat);
            threadBoundSysErr.installThreadStream(errformat);
            return this;
        }

        public void resetOutputStreams() {
            threadBoundSysOut.removeThreadStream();
            threadBoundSysErr.removeThreadStream();
            if (null != origout) {
                threadBoundSysOut.installThreadStream(origout);
            }
            if (null != origerr) {
                threadBoundSysErr.installThreadStream(origerr);
            }
        }
    }
}

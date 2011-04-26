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

import com.dtolabs.rundeck.core.cli.ExecTool;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.InterpreterException;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.utils.FormattedOutputStream;
import com.dtolabs.rundeck.core.utils.LogReformatter;
import com.dtolabs.rundeck.core.utils.MapGenerator;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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

    public ExecutionResult executeItem(ExecutionContext context, ExecutionItem item) throws ExecutionException {
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginExecution(context, item);
        }

        boolean success = false;
        DispatcherResult result = null;
        BaseExecutionResult baseExecutionResult = null;
        try {
            result = dispatchToNodes(context, item);
            success = result.isSuccess();
            baseExecutionResult = new BaseExecutionResult(result, success, null);
        } catch (DispatcherException e) {
            baseExecutionResult = new BaseExecutionResult(result, success, e);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishExecution(baseExecutionResult, context, item);
            }
        }

        return baseExecutionResult;
    }

    public InterpreterResult interpretCommand(ExecutionContext context,
                                              ExecutionItem item, INodeEntry node) throws InterpreterException {

        final CommandInterpreter interpreter;
        try {
            interpreter = framework.getCommandInterpreterForItem(item);
        } catch (ExecutionServiceException e) {
            throw new InterpreterException(e);
        }

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginInterpretCommand(context, item, node);
        }
        InterpreterResult result = null;
        try {
            result = interpreter.interpretCommand(context, item, node);
        } finally {
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishInterpretCommand(result, context, item, node);
            }
        }
        return result;
    }

    public DispatcherResult dispatchToNodes(ExecutionContext context, ExecutionItem item) throws
        DispatcherException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeDispatch(context, item);
        }
        final NodeDispatcher dispatcher;
        try {
            dispatcher = framework.getNodeDispatcherForContext(context);
        } catch (ExecutionServiceException e) {
            throw new DispatcherException(e);
        }
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

    public DispatcherResult dispatchToNodes(ExecutionContext context, Dispatchable item) throws
        DispatcherException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeDispatch(context, item);
        }
        final NodeDispatcher dispatcher;
        try {
            dispatcher = framework.getNodeDispatcherForContext(context);
        } catch (ExecutionServiceException e) {
            throw new DispatcherException(e);
        }
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


        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyFileStream(context, input, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        final LogReformatter formatter = createLogReformatter(node, context.getExecutionListener());
        final ThreadStreamFormatter loggingReformatter = new ThreadStreamFormatter(formatter).invoke();
        String result = null;
        try {
            result = copier.copyFileStream(context, input, node);
        } finally {
            loggingReformatter.resetOutputStreams();
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public String fileCopyFile(ExecutionContext context, File file,
                               INodeEntry node) throws FileCopierException {
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyFile(context, file, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        final LogReformatter formatter = createLogReformatter(node, context.getExecutionListener());
        final ThreadStreamFormatter loggingReformatter = new ThreadStreamFormatter(formatter).invoke();
        String result = null;
        try {
            result = copier.copyFile(context, file, node);
        } finally {
            loggingReformatter.resetOutputStreams();
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public String fileCopyScriptContent(ExecutionContext context, String script,
                                        INodeEntry node) throws FileCopierException {
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginFileCopyScriptContent(context, script, node);
        }
        final FileCopier copier;
        try {
            copier = framework.getFileCopierForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        final LogReformatter formatter = createLogReformatter(node, context.getExecutionListener());
        final ThreadStreamFormatter loggingReformatter = new ThreadStreamFormatter(formatter).invoke();
        String result = null;
        try {
            result = copier.copyScriptContent(context, script, node);
        } finally {
            loggingReformatter.resetOutputStreams();
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishFileCopy(result, context, node);
            }
        }
        return result;
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node) throws ExecutionException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeExecution(context, command, node);
        }
        final NodeExecutor nodeExecutor;
        try {
            nodeExecutor = framework.getNodeExecutorForNodeAndProject(node, context.getFrameworkProject());
        } catch (ExecutionServiceException e) {
            throw new ExecutionException(e);
        }

        //create node context for node and substitute data references in command
        final Map<String, Map<String, String>> nodeDataContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(node), context.getDataContext());
        final String[] nodeCommand = DataContextUtils.replaceDataReferences(command, nodeDataContext);

        final LogReformatter formatter = createLogReformatter(node, context.getExecutionListener());
        final ThreadStreamFormatter loggingReformatter = new ThreadStreamFormatter(formatter).invoke();
        NodeExecutorResult result = null;
        try {
            final ExecutionContextImpl nodeContext = ExecutionContextImpl.createExecutionContextImpl(context,
                nodeDataContext);
            result = nodeExecutor.executeCommand(nodeContext, nodeCommand, node);
        } finally {
            loggingReformatter.resetOutputStreams();
            if (null != context.getExecutionListener()) {
                context.getExecutionListener().finishNodeExecution(result, context, command, node);
            }
        }
        return result;
    }

    public String getName() {
        return SERVICE_NAME;
    }

    private static class ContextLoggerExecutionListenerMapGenerator implements MapGenerator<String,String>{
        final ContextLoggerExecutionListener ctxListener;

        private ContextLoggerExecutionListenerMapGenerator(final ContextLoggerExecutionListener ctxListener) {
            this.ctxListener = ctxListener;
        }

        public Map<String, String> getMap() {
            return ctxListener.getLoggingContext();
        }
    }
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
                baseContext.put("node", node.getNodename());
                baseContext.put("user", node.extractUserName());
                gen = new LogReformatter(logformat, baseContext);
            }

        }
        return gen;
    }

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

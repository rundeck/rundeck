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

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.InterpreterException;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherException;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.*;

import java.io.File;
import java.io.InputStream;

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
        Exception exception = null;
        DispatcherResult result = null;
        try {
            result = dispatchToNodes(context, item);
            success = result.isSuccess();
        } catch (DispatcherException e) {
            exception = e;
        }
        final BaseExecutionResult baseExecutionResult = new BaseExecutionResult(result, success, exception);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishExecution(baseExecutionResult, context, item);
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
        InterpreterResult result = interpreter.interpretCommand(context, item, node);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishInterpretCommand(result, context, item, node);
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
        DispatcherResult result = dispatcher.dispatch(context, item);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishNodeDispatch(result, context, item);
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
        DispatcherResult result = dispatcher.dispatch(context, item);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishNodeDispatch(result, context, item);
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
            copier = framework.getFileCopierForNode(node);
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        String result = copier.copyFileStream(context, input, node);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishFileCopy(result, context, node);
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
            copier = framework.getFileCopierForNode(node);
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        String result = copier.copyFile(context, file, node);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishFileCopy(result, context, node);
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
            copier = framework.getFileCopierForNode(node);
        } catch (ExecutionServiceException e) {
            throw new FileCopierException(e);
        }
        String result = copier.copyScriptContent(context, script, node);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishFileCopy(result, context, node);
        }
        return result;
    }

    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command,
                                             INodeEntry node) throws ExecutionException {

        if (null != context.getExecutionListener()) {
            context.getExecutionListener().beginNodeExecution(context, command, node);
        }
        final NodeExecutor nodeExecutor;
        try {
            nodeExecutor = framework.getNodeExecutorForNode(node);
        } catch (ExecutionServiceException e) {
            throw new ExecutionException(e);
        }
        NodeExecutorResult result = nodeExecutor.executeCommand(context, command, node);
        if (null != context.getExecutionListener()) {
            context.getExecutionListener().finishNodeExecution(result, context, command, node);
        }
        return result;
    }

    public String getName() {
        return SERVICE_NAME;
    }
}

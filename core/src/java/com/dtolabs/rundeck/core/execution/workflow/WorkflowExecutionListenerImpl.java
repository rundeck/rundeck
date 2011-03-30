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
* WorkflowExecutionListenerImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 3:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.CLIExecutionListener;
import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * WorkflowExecutionListenerImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class WorkflowExecutionListenerImpl extends CLIExecutionListener {
    public WorkflowExecutionListenerImpl(BuildListener buildListener, FailedNodesListener failedNodesListener,
                                         CLIToolLogger logger) {
        super(buildListener, failedNodesListener, logger);
    }

    @Override
    public void beginExecution(ExecutionContext context, ExecutionItem item) {
        super.beginExecution(context, item);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL, "beginExecution");
    }

    @Override
    public void finishExecution(ExecutionResult result, ExecutionContext context,
                                ExecutionItem item) {
        super.finishExecution(result, context, item);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL, "finishExecution");
    }

    @Override
    public void beginNodeExecution(ExecutionContext context, String[] command,
                                   INodeEntry node) {
        super.beginNodeExecution(context, command, node);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "beginNodeExec(" + node.getNodename() + "): " + Arrays.asList(command));
    }

    @Override
    public void finishNodeExecution(NodeExecutorResult result,
                                    ExecutionContext context, String[] command,
                                    INodeEntry node) {
        super.finishNodeExecution(result, context, command, node);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "finishNodeExecution(" + node.getNodename() + "): " + Arrays.asList(command));
    }

    @Override
    public void beginNodeDispatch(ExecutionContext context, ExecutionItem item) {
        super.beginNodeDispatch(context, item);
    }

    @Override
    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context,
                                   ExecutionItem item) {
        super.finishNodeDispatch(result, context, item);
    }

    @Override
    public void beginFileCopyFileStream(ExecutionContext context, InputStream input,
                                        INodeEntry node) {
        super.beginFileCopyFileStream(context, input, node);
    }

    @Override
    public void beginFileCopyFile(ExecutionContext context, File input,
                                  INodeEntry node) {
        super.beginFileCopyFile(context, input, node);
    }

    @Override
    public void beginFileCopyScriptContent(ExecutionContext context, String input,
                                           INodeEntry node) {
        super.beginFileCopyScriptContent(context, input, node);
    }

    @Override
    public void finishFileCopy(String result, ExecutionContext context,
                               INodeEntry node) {
        super.finishFileCopy(result, context, node);
    }

    @Override
    public void beginInterpretCommand(ExecutionContext context, ExecutionItem item,
                                      INodeEntry node) {
        super.beginInterpretCommand(context, item, node);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "beginInterpretCommand(" + node.getNodename() + "): " + item.getType());
    }

    @Override
    public void finishInterpretCommand(InterpreterResult result,
                                       ExecutionContext context, ExecutionItem item,
                                       INodeEntry node) {
        super.finishInterpretCommand(result, context, item, node);
        context.getExecutionListener().log(Constants.DEBUG_LEVEL,
            "finishInterpretCommand(" + node.getNodename() + "): " + item.getType() + ": " + result);
    }
}

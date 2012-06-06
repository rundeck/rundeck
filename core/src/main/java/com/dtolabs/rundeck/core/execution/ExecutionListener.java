/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* ExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 4:41:48 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;

/**
 * ExecutionListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ExecutionListener {
    /**
     * Return true if output should be terse and not prefixed
     *
     * @return
     */
    public boolean isTerse();

    /**
     * Return log message format
     *
     * @return
     */
    public String getLogFormat();

    /**
     * Log a message
     *
     * @param level   the log level
     * @param message Message being logged. <code>null</code> messages are not logged, however, zero-length strings
     *                are.
     */
    public void log(final int level, final String message);

    /**
     * Return a listener for failed node list
     *
     * @return listener
     */
    public FailedNodesListener getFailedNodesListener();


    /**
     * Called when executionb begins
     */
    public void beginExecution(ExecutionContext context, ExecutionItem item);

    /**
     * Called when execution finishes
     */
    public void finishExecution(ExecutionResult result, ExecutionContext context, ExecutionItem item);

    /**
     * Called before execution of command on node
     */
    public void beginNodeExecution(ExecutionContext context, String[] command, final INodeEntry node);

    /**
     * Called after execution of command on node.
     */
    public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                    final INodeEntry node);

    /**
     * Begin dispatch of command to set of nodes
     */
    public void beginNodeDispatch(ExecutionContext context, ExecutionItem item);
    /**
     * Begin dispatch of command to set of nodes
     */
    public void beginNodeDispatch(ExecutionContext context, Dispatchable item);

    /**
     * Finish node dispatch
     */
    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, ExecutionItem item);

    /**
     * Finish node dispatch
     */
    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item);

    /**
     * Begin file copy of stream
     */
    public void beginFileCopyFileStream(final ExecutionContext context, InputStream input, INodeEntry node);

    /**
     * Begin file copy of file
     */
    public void beginFileCopyFile(final ExecutionContext context, File input, INodeEntry node);

    /**
     * Begin file copy of string
     */
    public void beginFileCopyScriptContent(final ExecutionContext context, String input, INodeEntry node);

    /**
     * Finish file copy
     */
    public void finishFileCopy(String result, ExecutionContext context, INodeEntry node);

    /**
     * Begin command interpretation
     */
    public void beginInterpretCommand(ExecutionContext context, ExecutionItem item, INodeEntry node);

    /**
     * Finish command interpretation
     */
    public void finishInterpretCommand(InterpreterResult result, ExecutionContext context, ExecutionItem item,
                                       INodeEntry node);

    /**
     * Return an ExecutionListenerOverride that will delegate to this ExecutionListener, but allows overriding
     * property values.
     */
    public ExecutionListenerOverride createOverride();
}

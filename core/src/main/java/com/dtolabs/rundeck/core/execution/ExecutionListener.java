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
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.plugins.PluginLogger;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * ExecutionListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ExecutionListener extends PluginLogger {
    /**
     * @return true if output should be terse and not prefixed
     */
    public boolean isTerse();

    /**
     * @return log message format
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

    public void event(String eventType, final String message, final Map eventMeta);

    /**
     * Return a listener for failed node list
     *
     * @return listener
     */
    public FailedNodesListener getFailedNodesListener();



    /**
     * Called before execution of command on node
     * @param context context
     * @param command command strings
     * @param node node
     */
    public void beginNodeExecution(ExecutionContext context, String[] command, final INodeEntry node);

    /**
     * Called after execution of command on node.
     * @param result result
     * @param context context
     * @param command command strings
     * @param node node
     */
    public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                    final INodeEntry node);

    /**
     * Begin dispatch of command to set of nodes
     * @param context context
     * @param item step
     */
    public void beginNodeDispatch(ExecutionContext context, StepExecutionItem item);
    /**
     * Begin dispatch of command to set of nodes
     * @param context context
     * @param item dispatch
     */
    public void beginNodeDispatch(ExecutionContext context, Dispatchable item);

    /**
     * Finish node dispatch
     * @param result result
     * @param context context
     * @param item step
     */
    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item);

    /**
     * Finish node dispatch
     * @param result result
     * @param context context
     * @param item dispatch
     */
    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item);

    /**
     * Begin file copy of stream
     * @param context context
     * @param input input stream
     * @param node node
     */
    public void beginFileCopyFileStream(final ExecutionContext context, InputStream input, INodeEntry node);

    /**
     * Begin file copy of file
     * @param context context
     * @param input file
     * @param node node
     */
    public void beginFileCopyFile(final ExecutionContext context, File input, INodeEntry node);

    /**
     * Begin file copy of string
     * @param context context
     * @param input string
     * @param node node
     */
    public void beginFileCopyScriptContent(final ExecutionContext context, String input, INodeEntry node);

    /**
     * Finish file copy
     * @param result result
     * @param context context
     * @param node node
     */
    public void finishFileCopy(String result, ExecutionContext context, INodeEntry node);


    /**
     * @return an ExecutionListenerOverride that will delegate to this ExecutionListener, but allows overriding
     * property values.
     */
    public ExecutionListenerOverride createOverride();
}

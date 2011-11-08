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
* ContextExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/30/11 6:07 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;
import java.util.Map;

/**
 * ContextExecutionListener listens to execution actions, and logs messages to a ContextLogger.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ContextualExecutionListener implements ContextLoggerExecutionListener {
    private FailedNodesListener failedNodesListener;
    private ContextLogger logger;
    private boolean terse;
    private String logFormat;

    public ContextualExecutionListener(
        final FailedNodesListener failedNodesListener,
        final ContextLogger logger,
        final boolean terse,
        final String logFormat
    ) {

        this.failedNodesListener = failedNodesListener;
        this.terse = terse;
        this.logFormat = logFormat;
        this.logger = logger;
    }

    public final void log(final int level, final String message) {
        log(level, message, getLoggingContext());
    }

    /**
     * Method should be overridden to return appropriate logging context data
     */
    public Map<String, String> getLoggingContext() {
        return null;
    }

    public void log(final int level, final String message, Map<String, String> data) {
        if (level >= Constants.DEBUG_LEVEL) {
            logger.verbose(message, data);
        } else if (level >= Constants.VERBOSE_LEVEL) {
            logger.verbose(message, data);
        } else if (level >= Constants.INFO_LEVEL) {
            logger.log(message, data);
        } else if (level >= Constants.WARN_LEVEL) {
            logger.warn(message, data);
        } else if (level >= Constants.ERR_LEVEL) {
            logger.error(message, data);
        } else {
            logger.log(message, data);
        }
    }

    public void beginExecution(ExecutionContext context, ExecutionItem item) {
    }

    public void finishExecution(ExecutionResult result, ExecutionContext context, ExecutionItem item) {
    }

    public void beginNodeExecution(ExecutionContext context, String[] command, INodeEntry node) {
    }

    public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, String[] command,
                                    INodeEntry node) {
    }

    public void beginNodeDispatch(ExecutionContext context, ExecutionItem item) {
    }

    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, ExecutionItem item) {
    }

    public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
    }

    public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {
    }

    public void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {
    }

    public void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {
    }

    public void beginFileCopyScriptContent(ExecutionContext context, String input, INodeEntry node) {
    }

    public void finishFileCopy(String result, ExecutionContext context, INodeEntry node) {
    }

    public void beginInterpretCommand(ExecutionContext context, ExecutionItem item, INodeEntry node) {
    }

    public void finishInterpretCommand(InterpreterResult result, ExecutionContext context, ExecutionItem item,
                                       INodeEntry node) {
    }

    public FailedNodesListener getFailedNodesListener() {
        return failedNodesListener;
    }


    public boolean isTerse() {
        return terse;
    }

    public void setTerse(final boolean terse) {
        this.terse = terse;
    }

    public String getLogFormat() {
        return logFormat;
    }

    public void setLogFormat(String logFormat) {
        this.logFormat = logFormat;
    }

}

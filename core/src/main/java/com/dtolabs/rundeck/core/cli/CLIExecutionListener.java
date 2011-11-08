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
* CLIExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 8, 2010 11:33:05 AM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.commands.InterpreterResult;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;

/**
 * CLIExecutionListener implements ExecutionListener, and is used to supply other listeners to the ExecutionService,
 *  as well as provide a mechanism for logging messages to a provided CLIToolLogger.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CLIExecutionListener implements ExecutionListener {
    private FailedNodesListener failedNodesListener;
    private CLIToolLogger logger;
    private boolean terse;
    private int loglevel;
    private String logFormat;

    /**
     * Create the CLIExecutionListener
     *
     * @param failedNodesListener a listener for failed nodes list result
     * @param logger a logger
     */
    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger, final int loglevel) {
        this.failedNodesListener = failedNodesListener;
        this.logger = logger;
        this.loglevel=loglevel;
    }

    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger,
                                final int loglevel,
                                final boolean terse) {
        this.failedNodesListener = failedNodesListener;
        this.logger = logger;
        this.loglevel=loglevel;
        this.terse = terse;
    }

    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger,
                                final int loglevel,
                                final boolean terse, final String logFormat) {
        this.failedNodesListener = failedNodesListener;
        this.logger = logger;
        this.loglevel=loglevel;
        this.terse = terse;
        this.logFormat = logFormat;
    }

    /**
     * Return true if the logging level is enabled based on logger params
     *
     * @param level log level
     *
     * @return true if the level is enabled based on logging params
     */

    private boolean shouldlog(final int level) {
        return level <= loglevel;
    }

    public void log(final int level, final String message) {
        if (shouldlog(level)) {
            if (level >= Constants.DEBUG_LEVEL) {
                logger.verbose(message);
            } else if (level >= Constants.VERBOSE_LEVEL) {
                logger.verbose(message);
            } else if (level >= Constants.INFO_LEVEL) {
                logger.log(message);
            } else if (level >= Constants.WARN_LEVEL) {
                logger.warn(message);
            } else if (level >= Constants.ERR_LEVEL) {
                logger.error(message);
            } else {
                logger.log(message);
            }
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

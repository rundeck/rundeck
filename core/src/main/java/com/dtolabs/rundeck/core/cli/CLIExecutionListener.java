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
import com.dtolabs.rundeck.core.execution.*;

import java.util.Map;


/**
 * CLIExecutionListener implements ExecutionListener, and is used to supply other listeners to the ExecutionService,
 *  as well as provide a mechanism for logging messages to a provided CLIToolLogger.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class CLIExecutionListener extends ExecutionListenerOverrideBase  {
    private CLIToolLogger logger;
    private int loglevel;
    private CLIExecutionListener delegate;

    private CLIExecutionListener(CLIExecutionListener delegate) {
        super(delegate);
        this.delegate = delegate;
    }
    /**
     * Create the CLIExecutionListener
     *
     * @param failedNodesListener a listener for failed nodes list result
     * @param logger a logger
     *               @param loglevel level
     */
    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger, final int loglevel) {
        super(failedNodesListener,false,null);
        this.logger = logger;
        this.loglevel=loglevel;
    }

    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger,
                                final int loglevel,
                                final boolean terse) {
        super(failedNodesListener,terse, null);
        this.logger = logger;
        this.loglevel=loglevel;
    }

    public CLIExecutionListener(final FailedNodesListener failedNodesListener,
                                final CLIToolLogger logger,
                                final int loglevel,
                                final boolean terse, final String logFormat) {
        super(failedNodesListener,terse,logFormat);
        this.logger = logger;
        this.loglevel=loglevel;
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
        if (null != delegate) {
            delegate.log(level, message);
            return;
        }
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

    @Override
    public void event(String eventType, String message, Map eventMeta) {
        logger.debug("event[" + eventType + "] " + message);
    }

    public ExecutionListenerOverride createOverride() {
        return new CLIExecutionListener(this);
    }

}

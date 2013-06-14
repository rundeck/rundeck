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
import com.dtolabs.rundeck.core.execution.*;

import java.util.Map;

/**
 * ContextExecutionListener listens to execution actions, and logs messages to a ContextLogger.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ContextualExecutionListener extends ExecutionListenerOverrideBase implements ContextLoggerExecutionListener {
    private ContextLogger logger;
    private ContextualExecutionListener delegate;
    protected ContextualExecutionListener(ContextualExecutionListener delegate){
        super(delegate);
        this.delegate=delegate;
    }

    public ContextualExecutionListener(
        final FailedNodesListener failedNodesListener,
        final ContextLogger logger,
        final boolean terse,
        final String logFormat
    ) {
        super(failedNodesListener, terse, logFormat);
        this.logger = logger;
    }

    public final void log(final int level, final String message) {
        if(null!=delegate) {
            delegate.log(level, message);
        }else{
            log(level, message, getLoggingContext());
        }
    }

    public void log(final int level, final String message, Map<String, String> data) {
        if (null != delegate) {
            delegate.log(level, message, data);
            return ;
        }
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

    public ExecutionListenerOverride createOverride() {
        return new ContextualExecutionListener(this);
    }

    /**
     */
    public Map<String, String> getContext() {
        return getLoggingContext();
    }

}

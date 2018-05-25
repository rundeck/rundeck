/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.dtolabs.rundeck.core.logging.LogLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * ContextExecutionListener listens to execution actions, and logs messages to a ContextLogger.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ContextualExecutionListener extends ExecutionListenerOverrideBase {
    private ContextualExecutionListener delegate;
    private final ExecutionLogger logger;

    private boolean ignoreError;

    public void ignoreErrors(boolean value) {
        ignoreError = value;
    }
    protected ContextualExecutionListener(
            ContextualExecutionListener delegate,
            final ExecutionLogger logger
    )
    {
        super(delegate);
        this.delegate = delegate;
        this.logger = logger;
    }

    public ContextualExecutionListener(
            final FailedNodesListener failedNodesListener,
            final ExecutionLogger logger
    ) {
        super(failedNodesListener);
        this.logger = logger;
    }

    @Override
    public void event(String eventType, String message, Map eventMeta) {
        if (null != delegate) {
            delegate.event(eventType, message, eventMeta);
        } else if (null != logger) {
            logger.event(eventType, message, eventMeta);
        }
    }

    @Override
    public void log(int level, final String message) {
        if (ignoreError && level < Constants.INFO_LEVEL) {
            level = Constants.INFO_LEVEL;
        }
        if (null != delegate) {
            delegate.log(level, message);
        } else if (null != logger) {
            logger.log(level, message);
        }

    }

    @Override
    public void log(final int level, final String message, final Map eventMeta) {
        if (null != delegate) {
            delegate.log(level, message, eventMeta);
        } else if (null != logger) {
            logger.log(level, message, eventMeta);
        }
    }

    public ExecutionListenerOverride createOverride() {
        return new ContextualExecutionListener(this, logger);
    }

    public ExecutionLogger getLogger() {
        return logger;
    }
}

/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.logging.*;

import java.util.Map;

/**
 * @author greg
 * @since 5/10/17
 */
public interface LogFilterPlugin {
    /**
     * Initialize filtering for the current context
     */
    void init(PluginLoggingContext context);

    /**
     * Handle a log event
     *
     * @param event event
     *
     * @return true if the log event should be emitted normally, false to quell it
     */
    void handleEvent(LogFilterPlugin.Control control, LogEventControl event);

    /**
     * Called when the current step/node output is complete
     */
    void complete(LogFilterPlugin.Control control);

    public interface Control extends LogEventReceiver {
    }
}

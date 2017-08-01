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
 * Provides a filter mechanism for log events, with access to the execution context
 *
 * @author greg
 * @since 5/10/17
 */
public interface LogFilterPlugin {
    /**
     * Initialize filtering for the current context, optional
     */
    default void init(PluginLoggingContext context) {

    }

    /**
     * Handle a log event
     *
     * @param context the context for the plugin
     * @param event   event
     */
    void handleEvent(PluginLoggingContext context, LogEventControl event);

    /**
     * Called when the current step/node output is complete, optional
     *
     * @param context the context for the plugin
     */
    default void complete(PluginLoggingContext context) {

    }
}

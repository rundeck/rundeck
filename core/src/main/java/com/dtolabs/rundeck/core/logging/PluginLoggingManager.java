/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.logging;

import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin;

import java.util.function.Supplier;


/**
 * Allows installing plugins into the logging stream. The {@link #begin()} must be called before {@link #end()},
 * or the {@link #runWith(Supplier)} can be used to wrap another action with begin/end.
 *
 * @author greg
 * @since 5/11/17
 */
public interface PluginLoggingManager {

    /**
     * Begin using the plugins
     */
    void begin();

    /**
     * End using the plugins
     */
    void end(StatusResult result);

    /**
     * Run a function by wrapping the call with a begin/end using try/finally
     */
    <T extends StatusResult> T runWith(Supplier<T> supplier);
}

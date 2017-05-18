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

package com.dtolabs.rundeck.core.execution;

import java.util.Map;

/**
 * @author greg
 * @since 5/11/17
 */
public interface ExecutionLogger {

    /**
     * Log a message at a given level
     *
     * @param level   the log level, from 0 to 5, where 0 is "error" and 5 is "debug"
     * @param message Message being logged. <code>null</code> messages are not logged, however, zero-length strings
     *                are.
     */
    public void log(final int level, final String message);

    /**
     * Log a message at a given level, with additional metadata
     *
     * @param level     the log level, from 0 to 5, where 0 is "error" and 5 is "debug"
     * @param message   Message being logged. <code>null</code> messages are not logged, however, zero-length strings
     *                  are.
     * @param eventMeta metadata
     */
    public void log(final int level, final String message, final Map eventMeta);

    /**
     * @param eventType event type
     * @param message   Message being logged. <code>null</code> messages are not logged, however, zero-length strings
     *                  are.
     * @param eventMeta metadata
     */
    public void event(String eventType, final String message, final Map eventMeta);
}

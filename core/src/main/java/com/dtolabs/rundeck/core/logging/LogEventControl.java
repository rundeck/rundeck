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

package com.dtolabs.rundeck.core.logging;

import java.util.Map;

/**
 * Can modify an existing log event before being emitted
 *
 * @author greg
 * @since 5/11/17
 */
public interface LogEventControl extends LogEvent {
    /**
     * Set even type
     *
     * @param type new type
     *
     * @return this
     */
    LogEventControl setEventType(String type);

    /**
     * Force a certain log level
     *
     * @param level log level
     *
     * @return this
     */
    LogEventControl setLoglevel(LogLevel level);

    /**
     * set Message
     *
     * @param message new message
     *
     * @return this
     */
    LogEventControl setMessage(String message);

    /**
     * Add metadata
     *
     * @param data data to add
     *
     * @return this
     */
    LogEventControl addMetadata(Map<String, String> data);

    /**
     * Add a single item to metadata
     *
     * @param key key
     * @param value value
     *
     * @return this
     */
    LogEventControl addMetadata(String key, String value);

    /**
     * The log should be allowed to be emitted
     *
     * @return this
     */
    LogEventControl emit();

    /**
     * The log should be quelled from final writing, but not from further processing
     *
     * @return this
     */
    LogEventControl quell();

    /**
     * The log should be quieted in final writing, but does not modify the log level
     * for further processing
     *
     * @return this
     */
    LogEventControl quiet();

    /**
     * The event should be removed from processing by later filters
     *
     * @return this
     */
    LogEventControl remove();
}

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

import java.util.Date;
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
     * @param type
     *
     * @return
     */
    LogEventControl setEventType(String type);

    //    Date getDatetime();

    /**
     * Set level
     *
     * @param level
     *
     * @return
     */
    LogEventControl setLoglevel(LogLevel level);

    /**
     * set Message
     *
     * @param message
     *
     * @return
     */
    LogEventControl setMessage(String message);

    /**
     * Add metadata
     *
     * @param data
     *
     * @return
     */
    LogEventControl addMetadata(Map<String, String> data);

    /**
     * Add a single item to metadata
     *
     * @param key
     * @param value
     *
     * @return
     */
    LogEventControl addMetadata(String key, String value);

    /**
     * The log should be allowed to be emitted
     *
     * @return this
     */
    void emit();

    /**
     * The log should be quelled from final writing, but not from further processing
     *
     * @return this
     */
    void quell();

    /**
     * The event should be removed from processing
     *
     * @return
     */
    void remove();
}

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

package com.dtolabs.rundeck.core.logging;

import java.util.Date;
import java.util.Map;

/** $INTERFACE is ... User: greg Date: 5/23/13 Time: 2:38 PM */
class LogEventImpl implements LogEvent {
    private String eventType;
    private Date datetime;
    private LogLevel loglevel;
    private String message;
    private Map<String, String> metadata;

    private LogEventImpl(String eventType, Date datetime, LogLevel loglevel, String message, Map<String,
            String> metadata) {
        this.eventType = eventType;
        this.datetime = datetime;
        this.loglevel = loglevel;
        this.message = message;
        this.metadata = metadata;
    }

    public static LogEventImpl create(String eventType, Date datetime, LogLevel logLevel, String message, Map<String,
            String> metadata) {
        return new LogEventImpl(eventType, datetime, logLevel, message, metadata);
    }

    public String getEventType() {
        return eventType;
    }

    public Date getDatetime() {
        return datetime;
    }

    public LogLevel getLoglevel() {
        return loglevel;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}

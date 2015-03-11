package com.dtolabs.rundeck.core.logging;

import com.dtolabs.rundeck.core.execution.workflow.ContextLogger;

import java.util.Map;

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
 * ContextLogWriter.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 12:07 AM
 * 
 */

/**
 * Logs events to a writer using the context
 */
public class ContextLogWriter implements ContextLogger {
    StreamingLogWriter writer;

    public ContextLogWriter(StreamingLogWriter writer) {
        this.writer = writer;
    }

    public void log(String message, Map<String, String> context) {
        writer.addEvent(LogUtil.logNormal(message, context));
    }

    public void error(String message, Map<String, String> context) {
        writer.addEvent(LogUtil.logError(message, context));
    }

    public void warn(String message, Map<String, String> context) {
        writer.addEvent(LogUtil.logWarn(message, context));
    }

    public void verbose(String message, Map<String, String> context) {
        writer.addEvent(LogUtil.logVerbose(message, context));
    }

    public void debug(String message, Map<String, String> context) {
        writer.addEvent(LogUtil.logDebug(message, context));
    }

    @Override
    public void emit(String eventType, LogLevel level, String message, Map<String, String> context) {
        writer.addEvent(LogUtil.event(eventType, level, message, context));
    }

    public void log(String message) {
        log(message, null);
    }

    public void error(String message) {
        error(message, null);
    }

    public void warn(String message) {
        warn(message, null);
    }

    public void verbose(String message) {
        verbose(message, null);
    }

    public void debug(String message) {
        debug(message, null);
    }
}

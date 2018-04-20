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

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.execution.Contextual;
import com.dtolabs.rundeck.core.execution.ExecutionLogger;
import com.dtolabs.rundeck.core.logging.LogLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements ExecutionLogger via a context supplier and a logger
 * @author greg
 * @since 5/11/17
 */
public class LoggerWithContext implements ExecutionLogger {

    ContextLogger contextLogger;

    Contextual contextual;

    public LoggerWithContext(final ContextLogger contextLogger, final Contextual contextual) {
        this.contextLogger = contextLogger;
        this.contextual = contextual;
    }

    public final void log(final int level, final String message) {
        logInternal(level, message, contextual.getContext());
    }

    @Override
    public void log(final int level, final String message, final Map eventMeta) {
        logInternal(level, message, mergeMap(eventMeta, contextual.getContext()));
    }

    private Map<String, String> mergeMap(Map a, Map<String, String> b) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (null != a) {
            for (Object k : a.keySet()) {
                hashMap.put(k.toString(), a.get(k).toString());
            }
        }
        if (null != b) {
            hashMap.putAll(b);
        }
        return hashMap;
    }

    @Override
    public void event(String eventType, String message, Map eventMeta) {
        contextLogger.emit(eventType, LogLevel.NORMAL, message, mergeMap(eventMeta, contextual.getContext()));
    }

    public void logInternal(final int level, final String message, Map<String, String> data) {
        if (level >= Constants.DEBUG_LEVEL) {
            contextLogger.verbose(message, data);
        } else if (level >= Constants.VERBOSE_LEVEL) {
            contextLogger.verbose(message, data);
        } else if (level >= Constants.INFO_LEVEL) {
            contextLogger.log(message, data);
        } else if (level >= Constants.WARN_LEVEL) {
            contextLogger.warn(message, data);
        } else if (level >= Constants.ERR_LEVEL) {
            contextLogger.error(message, data);
        } else {
            contextLogger.log(message, data);
        }
    }

}

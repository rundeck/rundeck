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

package rundeck.services.logging

import com.dtolabs.rundeck.core.logging.FilterStreamingLogWriter
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * Log writer which contains a list of other writers
 */
class ExecutionLogWriter extends FilterStreamingLogWriter {
    public static final Logger log = Logger.getLogger(ExecutionLogWriter.class)

    /**
     *
     */
    File filepath

    ExecutionLogWriter(StreamingLogWriter writer) {
        super(writer)
    }
// utility methods
    void logError(String message) {
        addEvent(LogUtil.logError(message))
    }

    void log(String message) {
        addEvent(LogUtil.logNormal(message))
    }

    void logDebug(String message) {
        addEvent(LogUtil.logDebug(message))
    }

    void logWarn(String message) {
        addEvent(LogUtil.logWarn(message))
    }

    void logVerbose(String message) {
        addEvent(LogUtil.logVerbose(message))
    }
}

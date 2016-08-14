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
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Filters log events and includes only those below a LogLevel threshold
 */
class LoglevelThresholdLogWriter extends FilterStreamingLogWriter {
    LogLevel threshold

    LoglevelThresholdLogWriter(StreamingLogWriter writer, LogLevel threshold) {
        super(writer)
        this.threshold = threshold
    }

    @Override
    void addEvent(LogEvent event) {
        if (event.loglevel.belowThreshold(threshold)) {
            getWriter().addEvent(event)
        }
    }
}

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
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Used to log that a threshold was reached, and optionally truncate further output
 */
class ThresholdLogWriter extends FilterStreamingLogWriter {
    static final Logger LOG = LoggerFactory.getLogger(ThresholdLogWriter.class)

    static final String MSG_WARN_SIZE = "The log file size achieved more than {} bytes - current size: {} bytes"

    LoggingThreshold threshold
    final boolean truncate
    AtomicBoolean limitReached = new AtomicBoolean(false)
    AtomicBoolean warningReached = new AtomicBoolean(false)

    ThresholdLogWriter(final StreamingLogWriter writer, final LoggingThreshold threshold) {
        super(writer)
        this.threshold = threshold
        this.truncate = threshold.isTruncateOnLimitReached()
    }

    @Override
    void addEvent(final LogEvent event) {
        def limit = limitReached.get()
        boolean warning = warningReached.get()
        if (truncate && limit) {
            return
        }
        getWriter().addEvent(event)

        if (!warning && threshold.isWarningSizeReached() && warningReached.compareAndSet(false, true)) {
            LOG.warn(MSG_WARN_SIZE  , threshold.warningSize, threshold.getValue())
        }

        if (!limit && threshold.isThresholdExceeded() && limitReached.compareAndSet(false, true)) {
            String msgError = "Log output limit exceeded: ${threshold.description}"
            LOG.error(msgError)
            getWriter().addEvent(LogUtil.logError(msgError))
        }
    }
}

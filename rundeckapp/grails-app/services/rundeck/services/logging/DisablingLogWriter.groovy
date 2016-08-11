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
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import org.apache.log4j.Logger

/**
 * A {@link FilterStreamingLogWriter} that catches any throwables thrown by the underlying methods, and
 * simply disables further calls.
 */
class DisablingLogWriter extends FilterStreamingLogWriter {
    static Logger log = Logger.getLogger(DisablingLogWriter.class)
    boolean enabled = true
    private String identity

    static StreamingLogWriter create(StreamingLogWriter writer, String identity){
        return new DisablingLogWriter(writer,identity)
    }

    DisablingLogWriter(StreamingLogWriter writer, String identity) {
        super(writer)
        this.identity = identity
    }

    @Override
    void openStream() throws IOException {
        if (!enabled) return
        try {
            getWriter().openStream()
        } catch (Throwable e) {
            enabled = false
            log.error("Failed open stream for ${identity}, disabling. " + e.message)
            log.debug("Failed open stream for ${identity}, disabling. " + e.message, e)
        }
    }

    @Override
    void addEvent(LogEvent event) {
        if (!enabled) return
        try {
            getWriter().addEvent(event)
        } catch (Throwable e) {
            enabled = false
            log.error("Failed addEvent for ${identity}, disabling. " + e.message)
            log.debug("Failed addEvent for ${identity}, disabling. " + e.message, e)
        }
    }

    @Override
    void close() {
        if (!enabled) return
        try {
            getWriter().close()
        } catch (Throwable e) {
            enabled = false
            log.error("Failed close for ${identity}, disabling. " + e.message)
            log.debug("Failed close for ${identity}, disabling. " + e.message, e)
        }
    }
}

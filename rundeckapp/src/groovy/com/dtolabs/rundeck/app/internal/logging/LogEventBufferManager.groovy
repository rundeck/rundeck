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

package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

import java.nio.charset.Charset

/**
 * Creates log event buffers, and retains references to them, so that they can be
 * flushed later even if any thread-local references are lost when a thread finishes
 */
class LogEventBufferManager {
    Set<LogEventBuffer> buffers = new TreeSet<>()
    Charset charset

    LogEventBuffer create(Map context, Charset charset = null) {
        def buffer = new LogEventBuffer(context, charset ?: this.charset)
        buffers.add(buffer)
        return buffer
    }

    static LogEventBufferManager createManager(Charset charset = null) {
        new LogEventBufferManager(charset: charset)
    }

    /**
     * flush all incomplete event buffers in the appropriate order, then clear all buffers from the cache
     * @param writer
     * @param level
     */
    void flush(StreamingLogWriter writer, LogLevel level) {

        for (def b : buffers) {
            if (!b.isEmpty()) {
                writer.addEvent(b.createEvent(level))
            }
            b.clear()
        }
        buffers.clear()
    }
}

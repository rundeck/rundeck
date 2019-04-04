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

import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.utils.LogBuffer

import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicLong

/**
 * Buffer of log event data used by {@link com.dtolabs.rundeck.core.utils.ThreadBoundLogOutputStream}
 */
class LogEventBuffer implements Comparable, LogBuffer<LogEvent> {
    Date time
    Map<String, String> context
    Contextual listener
    ByteArrayOutputStream baos
    Long serial
    final static AtomicLong counter = new AtomicLong(0)
    private Charset charset
    private LogLevel level

    LogEventBuffer(LogLevel level, final Contextual listener, Charset charset = null) {
        this.level = level
        serial = counter.incrementAndGet()
        this.listener = listener
        reset(listener.context)
        this.charset = charset
    }

    boolean isEmpty() {
        time == null
    }

    void clear() {
        this.time = null
        this.context = null
        this.baos = new ByteArrayOutputStream()
    }

    void reset() {
        reset(listener.context)
    }
    void reset(final Map<String, String> context) {
        this.time = new Date()
        this.context = context
        this.baos = new ByteArrayOutputStream()
    }

    @Override
    void write(final byte b) {
        baos.write(b)
    }


    LogEvent get() {
        def string = baos?(charset?new String(baos.toByteArray(), (Charset) charset):new String(baos.toByteArray())):''
        return new DefaultLogEvent(
                loglevel: level,
                metadata: context ?: [:],
                message: string,
                datetime: time ?: new Date(),
                eventType: LogUtil.EVENT_TYPE_LOG
        )
    }

    @Override
    int compareTo(final Object o) {
        if (!(o instanceof LogEventBuffer)) {
            return -1;
        }
        LogEventBuffer other = (LogEventBuffer) o
        def td = compareDates(time, other.time)
        if (td != 0) {
            return td
        }
        return serial.compareTo(other.serial)
    }

    static int compareDates(Date a, Date b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a != null && b != null) {
            return a.compareTo(b)
        }

        return a != null ? -1 : 1

    }
}

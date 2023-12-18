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

package com.dtolabs.rundeck.core.logging.internal;

import com.dtolabs.rundeck.core.execution.Contextual;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.dtolabs.rundeck.core.utils.LogBufferManager;

import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.Set;
import java.util.TreeSet;

/**
 * Creates log event buffers, and retains references to them, so that they can be
 * flushed later even if any thread-local references are lost when a thread finishes
 */
public class LogEventBufferManager implements LogBufferManager<LogEvent,LogEventBuffer> {
    Set<LogEventBuffer> buffers = new TreeSet<>();
    Charset charset;
    Contextual listener;
    LogLevel level;

    public LogEventBufferManager(){}

    public LogEventBufferManager(final LogLevel level, final Contextual listener, final Charset charset) {
        this.charset = charset;
        this.listener = listener;
        this.level = level;
    }

    public LogEventBuffer create(Charset charset) {
        LogEventBuffer buffer = new LogEventBuffer(level,listener, charset != null ? charset: this.charset);
        buffers.add(buffer);
        return buffer;
    }

    public static LogEventBufferManager createManager(LogLevel level, Contextual listener, Charset charset) {
        return new LogEventBufferManager(level, listener, charset);
    }

    @Override
    public void flush(final Consumer<LogEvent> writer) {
        for (LogEventBuffer b : buffers) {
            if (!b.isEmpty()) {
                writer.accept(b.get());
            }
            b.clear();
        }
        buffers.clear();
    }
}

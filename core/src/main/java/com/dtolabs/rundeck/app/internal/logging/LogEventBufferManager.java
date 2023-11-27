/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 */
package com.dtolabs.rundeck.app.internal.logging;

import com.dtolabs.rundeck.core.execution.Contextual;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import com.dtolabs.rundeck.core.utils.LogBufferManager;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

public class LogEventBufferManager implements LogBufferManager<LogEvent,LogEventBuffer> {
    Set<LogEventBuffer> buffers = new TreeSet<>();
    Charset             charset;
    Contextual          listener;
    LogLevel            level;

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

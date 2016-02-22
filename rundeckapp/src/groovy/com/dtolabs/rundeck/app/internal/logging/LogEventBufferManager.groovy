package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Creates log event buffers, and retains references to them, so that they can be
 * flushed later even if any thread-local references are lost when a thread finishes
 */
class LogEventBufferManager {
    Set<LogEventBuffer> buffers = new TreeSet<>()

    LogEventBuffer create(Map context) {
        def buffer = new LogEventBuffer(context)
        buffers.add(buffer)
        return buffer
    }

    static LogEventBufferManager createManager() {
        new LogEventBufferManager()
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

package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Thread local buffered log output
 * 解决rundeck中文log乱码的问题，此文件在rundeck源文件路径:./rundeckapp/src/groovy/com/dtolabs/rundeck/app/internal/logging/ThreadBoundLogOutputStream.groovy
 * yeml#ucweb.com
 */
class ThreadBoundLogOutputStream extends OutputStream {
    StreamingLogWriter logger
    LogLevel level
    Contextual contextual
    ThreadLocal<Date> time = new ThreadLocal<Date>()
    ThreadLocal<Map> context = new ThreadLocal<Map>()
    ThreadLocal<Boolean> crchar = new ThreadLocal<Boolean>()
    ThreadLocal<ByteArrayOutputStream> baos = new ThreadLocal<ByteArrayOutputStream>()

    ThreadBoundLogOutputStream(StreamingLogWriter logger, LogLevel level, Contextual contextual) {
        this.logger = logger
        this.level = level
        this.contextual = contextual
    }

    public void write(final int b) {
        if (baos.get() == null) {
            createEventBuffer()
        }
        if (b == '\n') {
            flushEventBuffer();
            crchar.set(false);
        } else if (b == '\r') {
            crchar.set(true);
        } else {
            if (crchar.get()) {
                flushEventBuffer()
                crchar.set(false);
                createEventBuffer()
            }
            baos.get().write((byte) b)
        }

    }

    private void createEventBuffer() {
        time.set(new Date())
        context.set(contextual.getContext())
        baos.set(new ByteArrayOutputStream())
    }

    private void flushEventBuffer() {
        def buffer = baos.get()
        logger.addEvent(
                new DefaultLogEvent(
                        loglevel: level,
                        metadata: context.get() ?: [:],
                        message: buffer ? new String(buffer.toByteArray()) : '',
                        datetime: time.get() ?: new Date(),
                        eventType: LogUtil.EVENT_TYPE_LOG)
        )
        time.set(null)
        context.set(null)
        baos.set(null)
    }

    public void flush() {
        if (baos.get() != null && baos.get().size() > 0) {
            flushEventBuffer();
        }
    }
}

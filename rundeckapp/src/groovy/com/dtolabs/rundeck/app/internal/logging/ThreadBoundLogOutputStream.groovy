package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/**
 * Thread local buffered log output
 */
class ThreadBoundLogOutputStream extends OutputStream {
    StreamingLogWriter logger
    LogLevel level
    Contextual contextual
    ThreadLocal<StringBuilder> sb = new ThreadLocal<StringBuilder>()
    ThreadLocal<Date> time = new ThreadLocal<Date>()
    ThreadLocal<Map> context = new ThreadLocal<Map>()
    ThreadLocal<Boolean> crchar = new ThreadLocal<Boolean>()

    ThreadBoundLogOutputStream(StreamingLogWriter logger, LogLevel level, Contextual contextual) {
        this.logger = logger
        this.level = level
        this.contextual = contextual
    }
    public void write(final int b) {
        if (b == '\n') {
            event();
            crchar.set(false);
        } else if (b == '\r') {
            crchar.set(true);
        } else {
            if (crchar.get()) {
                event()
                crchar.set(false);
            }
            time.set(new Date())
            context.set(contextual.getContext())
            if(!sb.get()){
                sb.set(new StringBuilder())
            }
            sb.get().append((char) b)
        }

    }

    private void event() {
        logger.addEvent(
                new DefaultLogEvent(
                        loglevel: level,
                        metadata: context.get()?:[:],
                        message: sb.get().toString(),
                        datetime: time.get() ?: new Date(),
                        eventType: LogUtil.EVENT_TYPE_LOG)
        )
        sb.set(new StringBuilder())
        time.set(null)
        context.set(null)
    }

    public void flush() {
        if (sb.get().size() > 0) {
            event();
        }
    }
}

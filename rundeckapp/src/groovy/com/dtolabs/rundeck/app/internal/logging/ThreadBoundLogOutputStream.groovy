package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.execution.Contextual
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream

/**
 * Thread local buffered log output
 * 解决rundeck中文log乱码的问题，此文件在rundeck源文件路径:./rundeckapp/src/groovy/com/dtolabs/rundeck/app/internal/logging/ThreadBoundLogOutputStream.groovy
 * yeml#ucweb.com
 */
class ThreadBoundLogOutputStream extends OutputStream {
    StreamingLogWriter logger
    LogLevel level
    Contextual contextual
    ThreadLocal<LogEventBuffer> buffer = new ThreadLocal<LogEventBuffer>()
    InheritableThreadLocal<LogEventBufferManager> manager = new InheritableThreadLocal<LogEventBufferManager>()

    /**
     * Create a new thread local buffered stream
     * @param logger logger for events
     * @param level loglevel
     * @param contextual source of context
     */
    ThreadBoundLogOutputStream(StreamingLogWriter logger, LogLevel level, Contextual contextual) {
        this.logger = logger
        this.level = level
        this.contextual = contextual
    }

    /**
     * Install a new inherited thread local buffer manager and return it
     * @return manager
     */
    public LogEventBufferManager installManager() {
        def manager = LogEventBufferManager.createManager()
        this.manager.set(manager)
        return manager
    }

    /**
     * If no manager is set, install one, otherwise return the existing one
     * @return
     */
    LogEventBufferManager getOrCreateManager() {
        if (null == manager.get()) {
            installManager()
        }
        return manager.get()
    }

    /**
     * Write output
     * @param b
     */
    public void write(final int b) {
        def log = getOrReset()
        if (b == (char) '\n') {
            flushEventBuffer();
        } else if (b == (char) '\r') {
            log.crchar = true
        } else {
            if (log.crchar) {
                flushEventBuffer()
                resetEventBuffer()
            }
            log.baos.write((byte) b)
        }

    }

    /**
     * get the thread's event buffer, reset it if it is empty
     * @return
     */
    private LogEventBuffer getOrReset() {
        if (buffer.get() == null || buffer.get().isEmpty()) {
            resetEventBuffer()
        }
        return buffer.get()
    }

    /**
     * reset existing or create a new buffer with the current context
     */
    private void resetEventBuffer() {
        if (!buffer.get()) {
            buffer.set(getOrCreateManager().create(contextual.getContext()))
        } else {
            buffer.get().reset(contextual.getContext())
        }
    }

    /**
     * emit a log event for the current contents of the buffer
     */
    private void flushEventBuffer() {
        def logstate = buffer.get()
        logger.addEvent(logstate.createEvent(level))
        logstate.clear()
    }

    /**
     * Flush all event buffers managed by the current manager
     */
    public void flushBuffers() {
        getOrCreateManager().flush(logger, level)
    }

    public void flush() {
    }

    @Override
    void close() throws IOException {
        flushBuffers()

        super.close()
    }
}

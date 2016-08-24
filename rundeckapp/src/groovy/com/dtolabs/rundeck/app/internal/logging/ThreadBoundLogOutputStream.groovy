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
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream

import java.nio.charset.Charset

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
    InheritableThreadLocal<Charset> charset = new InheritableThreadLocal<Charset>()

    /**
     * Create a new thread local buffered stream
     * @param logger logger for events
     * @param level loglevel
     * @param contextual source of context
     */
    ThreadBoundLogOutputStream(StreamingLogWriter logger, LogLevel level, Contextual contextual, Charset charset=null) {
        this.logger = logger
        this.level = level
        this.contextual = contextual
        this.charset.set(charset)
    }
    /**
     * Set the charset to use
     * @param charset new charset
     * @return previous charset
     */
    public Charset setCharset(Charset charset) {
        Charset prev=this.charset.get()
        this.charset.set(charset)
        return prev
    }

    /**
     * Install a new inherited thread local buffer manager and return it
     * @return manager
     */
    public LogEventBufferManager installManager() {
        def manager = LogEventBufferManager.createManager(charset.get())
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

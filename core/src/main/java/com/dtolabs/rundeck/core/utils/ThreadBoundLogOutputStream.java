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

package com.dtolabs.rundeck.core.utils;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Thread local buffered log output
 */
public class ThreadBoundLogOutputStream<D, T extends LogBuffer<D>>
        extends OutputStream
{
    private Consumer<D> logger;
    private ThreadLocal<Holder<T>> buffer = new ThreadLocal<>();
    private InheritableThreadLocal<LogBufferManager<D,T>> manager = new InheritableThreadLocal<>();
    private InheritableThreadLocal<Charset> charset = new InheritableThreadLocal<>();
    private Function<Charset, LogBufferManager<D,T>> factory;

    @Data
    @RequiredArgsConstructor
    private static class Holder<X extends LogBuffer> {
        final X buffer;
        boolean crchar;

        public void clear() {
            crchar = false;
            buffer.clear();
        }

        public void reset() {
            crchar = false;
            buffer.reset();
        }
    }
    /**
     * Create a new thread local buffered stream
     * @param logger logger for events
     */
    public ThreadBoundLogOutputStream(
            Consumer<D> logger,
            Charset charset,
            Function<Charset, LogBufferManager<D,T>> factory
    )
    {
        this.logger = logger;
        this.charset.set(charset);
        this.factory = factory;
    }
    /**
     * Set the charset to use
     * @param charset new charset
     * @return previous charset
     */
    public Charset setCharset(Charset charset) {
        Charset prev = this.charset.get();
        this.charset.set(charset);
        return prev;
    }

    /**
     * Install a new inherited thread local buffer manager and return it
     * @return manager
     */
    public LogBufferManager<D,T> installManager() {
        LogBufferManager<D,T> manager = factory.apply(charset.get());
        this.manager.set(manager);
        return manager;
    }

    /**
     * If no manager is set, install one, otherwise return the existing one
     * @return
     */
    private LogBufferManager<D,T> getOrCreateManager() {
        if (null == manager.get()) {
            installManager();
        }
        return manager.get();
    }

    /**
     * Write output
     * @param b
     */
    public void write(final int b) {
        Holder<T> log = getOrReset();
        if (b == '\n') {
            flushEventBuffer();
        } else if (b == '\r') {
            log.setCrchar(true);
        } else {
            if (log.isCrchar()) {
                flushEventBuffer();
                resetEventBuffer();
            }
            log.getBuffer().write((byte) b);
        }

    }

    /**
     * get the thread's event buffer, reset it if it is empty
     * @return
     */
    private Holder<T> getOrReset() {
        if (buffer.get() == null || buffer.get().getBuffer().isEmpty()) {
            resetEventBuffer();
        }
        return buffer.get();
    }

    /**
     * reset existing or create a new buffer with the current context
     */
    private void resetEventBuffer() {
        if (buffer.get() == null) {
            buffer.set(new Holder<>(getOrCreateManager().create(charset.get())));
        } else {
            buffer.get().reset();
        }
    }

    /**
     * emit a log event for the current contents of the buffer
     */
    private void flushEventBuffer() {
        Holder<T> holder = buffer.get();
        logger.accept(holder.getBuffer().get());
        holder.clear();
    }

    /**
     * Flush all event buffers managed by the current manager
     */
    public void flushBuffers() {
        getOrCreateManager().flush(logger);
    }

    public void flush() {
    }

    @Override
    public void close() throws IOException {
        flushBuffers();

        super.close();
    }
}

/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.plugins;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author greg
 * @since 3/8/17
 */
public class Closeables {
    /**
     * A closeable which does nothing
     */
    public static final Closeable NO_OP = new Closeable() {
        @Override
        public void close() throws IOException {

        }
    };

    public static <T> CloseableProvider<T> closeableProvider(final T object) {
        return closeableProvider(object, null);
    }

    public static Closeable closeOnce(final Closeable closer) {
        return new Closeable() {
            final AtomicBoolean closed = new AtomicBoolean(false);

            @Override
            public void close() throws IOException {
                if (closed.compareAndSet(false, true)) {
                    closer.close();
                }
            }
        };

    }

    public static <T> CloseableProvider<T> closeableProvider(final T object, final Closeable closer) {
        return new CloseableProvider<T>() {
            @Override
            public T getProvider() {
                return object;
            }

            @Override
            public void close() throws IOException {
                if (null != closer) {
                    closer.close();
                }
            }
        };
    }

    /**
     * If the input object is a Closeable, return it, otherwise return a non-null Closeable which does nothing.
     *
     * @param object object which might be closeable
     *
     * @return a closeable
     */
    public static Closeable maybeCloseable(Object object) {

        if (object instanceof Closeable) {
            return (Closeable) object;
        } else {
            return NO_OP;
        }
    }

    /**
     * Create a single Closeable which closes all of the specified closeables
     *
     * @param throwIOException if true, throw any IOException encountered
     * @param closeables       collection of closeables
     *
     * @return closeable
     */
    public static Closeable single(boolean throwIOException, final Collection<Closeable> closeables) {
        return single(throwIOException, closeables.toArray(new Closeable[closeables.size()]));
    }


    /**
     * Create a single Closeable which closes all of the specified closeables and throws IOExceptions
     *
     * @param closeables closeables
     *
     * @return closeable which silently closes all in the collection
     */
    public static Closeable single(final Collection<Closeable> closeables) {
        return single(true, closeables);
    }

    /**
     * Create a single Closeable which closes all of the specified closeables and throws IOExceptions
     *
     * @param closeables closeables
     *
     * @return closeable which silently closes all in the array
     */
    public static Closeable single(final Closeable... closeables) {
        return single(true, closeables);
    }

    /**
     * Create a single Closeable which closes all of the specified closeables
     *
     * @param throwIOException if true, throw the final IOException encountered
     * @param closeables       array of closeables
     *
     * @return closeable
     */
    public static Closeable single(final boolean throwIOException, final Closeable... closeables) {
        return single(throwIOException, true, closeables);
    }

    /**
     * Create a single Closeable which closes all of the specified closeables
     *
     * @param throwIOException if true, throw an IOException if encountered, otherwise do not throw anything
     * @param throwLast        if true, only throw the last IOException encountered, otherwise throw the first
     * @param closeables       array of closeables
     *
     * @return closeable
     */
    public static Closeable single(
            final boolean throwIOException,
            final boolean throwLast,
            final Closeable... closeables
    )
    {
        return new Closeable() {

            @Override
            public void close() throws IOException {
                closeAll(throwIOException, throwLast, closeables);
            }
        };
    }

    public static void closeQuietly(final Collection<Closeable> closeables) {

        if (null == closeables) {
            return;
        }
        closeQuietly(closeables.toArray(new Closeable[closeables.size()]));
    }

    public static void closeAll(final Collection<Closeable> closeables) throws IOException {

        if (null == closeables) {
            return;
        }
        closeAll(closeables.toArray(new Closeable[closeables.size()]));
    }

    /**
     * Close all closeables and throw any final exception
     *
     * @param closeables
     */
    public static void closeAll(final Closeable... closeables) throws IOException {
        closeAll(true, true, closeables);
    }

    /**
     * Close all closeables quietly
     *
     * @param closeables
     */
    public static void closeQuietly(final Closeable... closeables) {
        try {
            closeAll(false, false, closeables);
        } catch (IOException e) {

        }
    }

    public static void closeAll(
            final boolean throwIOException,
            final boolean throwLast,
            final Closeable... closeables
    )
            throws IOException
    {
        if (null == closeables) {
            return;
        }
        IOException last = null;
        for (Closeable closeable : closeables) {
            if (null != closeable) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    last = e;
                    if (throwIOException && !throwLast) {
                        throw e;
                    }
                }
            }
        }
        if (throwIOException && null != last) {
            throw last;
        }
    }
}

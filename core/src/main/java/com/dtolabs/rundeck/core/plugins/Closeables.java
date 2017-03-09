/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author greg
 * @since 3/8/17
 */
public class Closeables {

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
}

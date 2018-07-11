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
import java.util.function.Function;

/**
 * A Loaded plugin provider which can be closed when no longer needed
 *
 * @author greg
 * @since 3/8/17
 */
public interface CloseableProvider<T>
    extends Closeable
{
    /**
     * @return the provided instance
     */
    T getProvider();

    /**
     * Convert a closeable provider of one type to a closeable of another type given a conversion function. The returned
     * closeable will manage closing this original closeable as well as the converted object if it is also closeable.
     *
     * @param converter converter function
     */
    default <X> CloseableProvider<X> convert(Function<T, X> converter) {
        X result = converter.apply(getProvider());
        //wrap the built source into another closeable provider
        return Closeables.closeableProvider(
            result,
            //close both the result if it is closeable, and this original closeable reference, when
            // released
            Closeables.single(Closeables.maybeCloseable(result), this)
        );
    }
}

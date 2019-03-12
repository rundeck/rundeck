/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * Manager for log buffers for a data type
 *
 * @param <D> datatype
 * @param <T> log buffer type
 */
public interface LogBufferManager<D, T extends LogBuffer<D>> {
    /**
     * Create a new log buffer with the charset
     *
     * @param charset charset
     */
    T create(Charset charset);

    /**
     * Flush all buffers with the consumer
     *
     * @param writer consumer of log events
     */
    void flush(Consumer<D> writer);
}

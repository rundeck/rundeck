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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Managers string log buffers
 */
public class StringLogManager
        implements LogBufferManager<String, StringLogBuffer>

{
    private List<StringLogBuffer> buffers = new ArrayList<>();
    private Charset charset;

    public StringLogManager(final Charset charset) {
        this.charset = charset;
    }

    @Override
    public StringLogBuffer create(final Charset charset) {
        StringLogBuffer buffer = new StringLogBuffer(charset != null ? charset : this.charset);
        buffers.add(buffer);
        return buffer;
    }

    @Override
    public void flush(final Consumer<String> writer) {
        for (StringLogBuffer buffer : buffers) {
            writer.accept(buffer.get());
            buffer.clear();
        }
        buffers.clear();
    }

    public static StringLogManager factory(Charset charset) {
        return new StringLogManager(charset);
    }
}

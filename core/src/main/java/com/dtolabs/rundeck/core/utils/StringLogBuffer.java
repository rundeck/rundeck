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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Implements basic buffer of data into a String
 */
public class StringLogBuffer
        implements LogBuffer<String>
{
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private final Charset charset;

    public StringLogBuffer(final Charset charset) {
        this.charset = charset;
    }

    @Override
    public boolean isEmpty() {
        return baos.size() == 0;
    }

    @Override
    public void reset() {
        baos = new ByteArrayOutputStream();
    }

    @Override
    public void write(final byte b) {
        baos.write(b);
    }

    @Override
    public void clear() {
        reset();
    }

    /**
     * @return contents as string
     */
    public String get() {
        return charset != null ? new String(baos.toByteArray(), charset) : new String(baos.toByteArray());
    }

}

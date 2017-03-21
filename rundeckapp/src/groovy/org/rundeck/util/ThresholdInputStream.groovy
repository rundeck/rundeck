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

package org.rundeck.util

/**
 * Reads the underlying input stream, throws {@link ThresholdInputStream.Threshold} if more than the max
 * number of bytes is read
 * @author greg
 * @since 2/23/17
 */
class ThresholdInputStream extends FilterInputStream {
    long max
    long total = 0

    /**
     * @param input input
     * @param max maximum number of bytes to read before throwing an exception
     */
    ThresholdInputStream(final InputStream input, long max) {
        super(input)
        this.max = max
    }


    @Override
    int read() throws IOException {
        int read = super.read()
        if (read < 0) {
            return read
        }
        update 1
        read
    }

    @Override
    int read(final byte[] b, final int off, final int len) throws IOException {
        int read = super.read(b, off, len)
        if (read < 0) {
            return read
        }
        update read
        read
    }

    private void update(int read) {
        total += read
        if (total > max) {
            throw new Threshold(total)
        }
    }

    static class Threshold extends IOException {
        long breach

        Threshold(long breach) {
            super()
            this.breach = breach
        }
    }
}

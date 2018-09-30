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

package com.dtolabs.utils;

import com.dtolabs.rundeck.core.logging.OffsetIterator;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Queue;

/*
 * FSFileLineIterator.java
 *
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 1/23/13 7:48 PM
 *
 */

/**
 * Iterate the lines in a stream or file, provide offset location of the current read position, supports file stream
 * which already has an offset
 */
public class StreamLineIterator
    implements OffsetIterator<String>
{

    private InputStreamReader read;
    private long offset;
    private Queue<String> buffer = new ArrayDeque<String>();
    private String encoding;
    boolean closed = false;

    /**
     * @param raf file steram
     * @param encoding optional encoding
     * @throws IOException on error
     */
    public StreamLineIterator(FileInputStream raf, String encoding) throws IOException {
        this(raf, raf.getChannel().position(), encoding);
    }

    /**
     *
     * @param stream text stream
     * @param encoding encoding
     * @throws IOException on error
     */
    public StreamLineIterator(InputStream stream, String encoding) throws IOException {
        this(stream, 0, encoding);
    }

    private StreamLineIterator(InputStream stream, long initialOffset, String encoding)
        throws IOException
    {
        this.encoding = encoding;
        offset = initialOffset;
        read = encoding != null ? new InputStreamReader(stream, encoding) : new InputStreamReader(stream);
        readNext();
    }

    @Override
    public boolean hasNext() {
        return buffer.size() > 0;
    }

    @Override
    public String next() {
        String s = buffer.remove();
        try {
            offset += (s.getBytes(encoding).length) + 1;
            readNext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return s;
    }

    private void readNext() throws IOException {
        if (closed) {
            throw new IllegalStateException("Stream is closed");
        }
        String v = readLine(read);
        if (null != v) {
            buffer.add(v);
        }
    }

    static String readLine(final Reader reader) throws IOException {

        StringBuilder line = new StringBuilder(200);
        int ch = reader.read();
        if (ch < 0) {
            return null;
        }
        //append chars until eol
        while (ch > 0 && ch != '\n' && ch != '\r') {
            line.append((char) ch);
            ch = reader.read();
        }
        return line.toString();
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        read.close();
        closed = true;
    }
}

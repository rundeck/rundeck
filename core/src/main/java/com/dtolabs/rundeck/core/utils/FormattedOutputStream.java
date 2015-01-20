/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PrefixedOutputStrea.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: May 26, 2010 10:54:42 AM
* $Id$
*/
package com.dtolabs.rundeck.core.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * FormattedOutputStream buffers the output data and then outputs each line after reformatting the line using the
 * specified Reformatter.  Context Data can be set via {@link #setContext(String, String)}.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 * @deprecated
 */
public class FormattedOutputStream extends FilterOutputStream {
    Reformatter reformatter;
    StringBuffer buffer;
    private final OutputStream originalSink;
    private HashMap<String, String> context;

    /**
     * Create a new FormattedOutputStream
     *
     * @param reformatter reformatter or null
     * @param sink        destination OutputStream
     */
    public FormattedOutputStream(final Reformatter reformatter, final OutputStream sink) {
        super(sink);
        this.originalSink=sink;
        this.reformatter = reformatter;
        buffer = new StringBuffer();
        context = new HashMap<String, String>();
    }

    /**
     * Return the reformatted message, or the original if no reformatter is specified.
     * @param message input message
     * @return reformatted result
     */
    private String formatMessage(final String message) {
        if (null != reformatter) {
            return reformatter.reformat(context, message);
        } else {
            return message;
        }
    }

    /**
     * Overrides the FilterOutputStream method to buffer the data.  When a newline is
     * encountered the buffered data is reformatted and written to the underlying output stream.
     * @param b char
     * @throws IOException on io error
     */
    public void write(final int b) throws IOException {
        if (b == '\n') {
            writeBufferedData();
        } else {
            buffer.append((char) b);
        }

    }

    /**
     * If the buffer has content, then reformat and write the content to the output stream.
     * @throws IOException
     */
    private void writeBufferedData() throws IOException {
        if (buffer.length() > 0) {
            out.write(formatMessage(buffer.toString()).getBytes());
            out.write('\n');
            buffer = new StringBuffer();
        }
    }

    @Override
    /**
     * writes buffered data prior to close
     */
    public void close() throws IOException {
        writeBufferedData();
        super.close();
    }

    /**
     * @return the context map
     */
    public Map<String, String> getContext() {
        return context;
    }

    /**
     * Set a context data value to be used by the Reformatter
     * @param key data key
     * @param value data value
     */
    public void setContext(final String key, final String value) {
        context.put(key, value);
    }

    /**
     * Return original OutputStream sink
     * @return original sink
     */
    public OutputStream getOriginalSink() {
        return originalSink;
    }
}

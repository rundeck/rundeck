package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

/*
 * Copyright 2013 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/**
 * Logs to a file using the OutputLogFormat
 */
class FSStreamingLogWriter implements StreamingLogWriter {
    static final String lineSep = "\n"
    private OutputStream output
    private Map<String, String> defaultMeta
    private OutputLogFormat formatter
    private boolean started
    private volatile long bytesWritten

    public long getBytesWritten(){
        return bytesWritten
    }

    /**
     * Create a FSStreamingLogWriter
     * @param output outputstream
     * @param threshold highest log level to emit
     * @param defaultMeta default metadata to add to emitted events, only applies if the metadata is not present
     * @param formatter log format
     */
    public FSStreamingLogWriter(OutputStream output, Map<String, String> defaultMeta,
                                OutputLogFormat formatter) {
        this.output = output
        this.defaultMeta = defaultMeta
        this.formatter = formatter
        started = false
        bytesWritten = 0
    }
    private write(String val) {
        def bytes = val.getBytes("UTF-8")
        output.write(bytes)
        bytesWritten += bytes.length
    }
    @Override
    void openStream() throws IOException{
        synchronized (this) {
            if (!started) {
                write(formatter.outputBegin())
                write(lineSep)
                started = true
            }
        }
    }

    @Override
    void addEvent(LogEvent event) {
        synchronized (this) {
            if (null == output) {
                throw new IllegalStateException("output was closed")
            }
            def event1 = formatter.outputEvent(new DefaultLogEvent(event, defaultMeta))
            write(event1)
            write(lineSep)
        }
    }

    void close() {
        synchronized (this) {
            if (null != output) {
                write(formatter.outputFinish())
                write(lineSep)
                output.flush()
                output.close()
                output = null
            }
        }
    }
}

package com.dtolabs.rundeck.app.internal.logging

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.StreamingLogWriter

import java.text.SimpleDateFormat

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
    static final String lineSep = System.getProperty("line.separator")
    private OutputStream output
    private Map<String, String> defaultMeta
    private OutputLogFormat formatter
    private LogLevel threshold
    private boolean started

    /**
     * Create a FSStreamingLogWriter
     * @param output outputstream
     * @param threshold highest log level to emit
     * @param defaultMeta default metadata to add to emitted events, only applies if the metadata is not present
     * @param formatter log format
     */
    public FSStreamingLogWriter(OutputStream output, LogLevel threshold, Map<String, String> defaultMeta,
                                OutputLogFormat formatter) {
        this.output = output
        this.defaultMeta = defaultMeta
        this.formatter = formatter
        this.threshold = threshold
        started = false
    }

    @Override
    void openStream(Map<String, Object> context) {
        synchronized (this) {
            if (!started) {
                output << formatter.outputBegin()
                output << lineSep
                started = true
            }
        }
    }

    @Override
    void addEntry(LogEvent event) {
        if (event.logLevel.belowThreshold(threshold)) {
            synchronized (this) {
                if (null == output) {
                    throw new IllegalStateException("output was closed")
                }
                def event1 = formatter.outputEvent(new DefaultLogEvent(event, defaultMeta))
                output << event1
                output << lineSep
            }
        }
    }

    void close() {
        synchronized (this) {
            if (null != output) {
                output << formatter.outputFinish()
                output << lineSep
                output.flush()
                output.close()
                output = null
            }
        }
    }
}

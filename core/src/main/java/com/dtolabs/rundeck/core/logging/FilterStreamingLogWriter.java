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

package com.dtolabs.rundeck.core.logging;

import java.io.IOException;

/**
 * A log writer class which can easily be subclassed override default behavior. By default all method calls are delegated
 * to another {@link StreamingLogWriter}
 */
public class FilterStreamingLogWriter implements StreamingLogWriter {
    private StreamingLogWriter writer;

    public FilterStreamingLogWriter(StreamingLogWriter writer) {
        this.writer = writer;
    }

    public void openStream() throws IOException {
        getWriter().openStream();
    }

    public void addEvent(LogEvent event) {
        getWriter().addEvent(event);
    }

    public void close() {
        getWriter().close();
    }

    public StreamingLogWriter getWriter() {
        return writer;
    }
}

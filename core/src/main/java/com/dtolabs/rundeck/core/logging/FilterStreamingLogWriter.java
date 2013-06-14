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

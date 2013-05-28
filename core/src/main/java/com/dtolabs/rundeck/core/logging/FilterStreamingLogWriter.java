package com.dtolabs.rundeck.core.logging;

import java.io.IOException;
import java.util.Map;

/**
 * A log writer class which can easily be subclassed override default behavior. By default all method calls are delegated
 * to another {@link StreamingLogWriter}
 */
public class FilterStreamingLogWriter implements StreamingLogWriter {
    private StreamingLogWriter writer;

    public FilterStreamingLogWriter(StreamingLogWriter writer) {
        this.writer = writer;
    }

    public void openStream(Map<String, ? extends Object> context) throws IOException {
        getWriter().openStream(context);
    }

    public void addEntry(LogEvent entry) {
        getWriter().addEntry(entry);
    }

    public void close() {
        getWriter().close();
    }

    public StreamingLogWriter getWriter() {
        return writer;
    }
}

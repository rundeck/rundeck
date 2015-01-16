package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.StreamingLogWriter;

import java.util.Map;

/**
 * Plugin interface for streaming log writers
 */
public interface StreamingLogWriterPlugin extends StreamingLogWriter {
    /**
     * Sets the execution context information for the log information being written, will be called prior to other
     * methods {@link #openStream()}
     *
     * @param context context data
     */
    public void initialize(Map<String, ? extends Object> context);
}

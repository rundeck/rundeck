package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.StreamingLogReader;

import java.util.Map;

/**
 * Plugin interface for streaming log readers
 */
public interface StreamingLogReaderPlugin extends StreamingLogReader {
    /**
     * Sets the execution context information for the log information being requested, will be called
     * prior to other methods {@link #openStream(Long)}
     * @param context
     */
    public void initialize(Map<String, ? extends Object> context);

}

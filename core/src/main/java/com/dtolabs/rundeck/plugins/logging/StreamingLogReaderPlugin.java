package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.StreamingLogReader;

import java.util.Map;

/**
 * Plugin interface for streaming log readers
 */
public interface StreamingLogReaderPlugin extends StreamingLogReader {
    /**
     * Sets the execution context information for the log information being requested, will be called
     * prior to other methods {@link #openStream(Long)}, and must return true to indicate the stream is ready to be open, false otherwise.
     * @param context execution context data
     * @return true if the stream is ready to open
     */
    public boolean initialize(Map<String, ? extends Object> context);

}

package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Represents a stream of data to store and metadata about it.
 */
public interface ContentMeta extends HasInputStream{
    /**
     * Return the metadata about the content
     * @return the metadata
     */
    Map<String, String> getMeta();
}

package org.rundeck.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Represents a stream of data to store and metadata about it.
 */
public interface ContentMeta {
    /**
     * Return the metadata about the content
     * @return the metadata
     */
    Map<String, String> getMeta();

    /**
     * Return the content stream
     * @return the content stream
     * @throws IOException
     */
    InputStream readContent() throws IOException;
}

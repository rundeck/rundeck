package com.dtolabs.rundeck.core.logging;

import java.io.InputStream;
import java.util.Date;

/**
 * Represents a file to be stored
 */
public interface StorageFile {
    /**
     * @return File type
     */
    String getFiletype();

    /**
     * @return input stream
     */
    InputStream getInputStream();

    /**
     * @return content length
     */
    long getLength();

    /**
     * @return last modified date
     */
    Date getLastModified();
}

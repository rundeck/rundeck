package com.dtolabs.rundeck.core.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Handles log file storage and retrieval
 */
public interface LogFileStorage {
    /**
     * Stores a log file read from the given stream
     *
     * @param stream the input stream
     * @param length the file length
     * @param lastModified the file modification time
     *
     * @return true if successful
     *
     * @throws IOException
     */
    boolean store(InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException;

    /**
     * Writes a log file to the given stream
     *
     * @param stream the output stream
     *
     * @return true if successful
     *
     * @throws IOException
     */
    boolean retrieve(OutputStream stream) throws IOException, LogFileStorageException;
}

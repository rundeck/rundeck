package com.dtolabs.rundeck.core.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Handles log file storage and retrieval
 * @deprecated no longer used, replaced by {@link ExecutionFileStorage}
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
     * @throws IOException if an io error occurs
     *
     * @throws com.dtolabs.rundeck.core.logging.LogFileStorageException if other errors occur
     */
    boolean store(InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException;

    /**
     * Writes a log file to the given stream
     *
     * @param stream the output stream
     *
     * @return true if successful
     *
     * @throws IOException if an io error occurs
     * @throws com.dtolabs.rundeck.core.logging.LogFileStorageException if other errors occur
     */
    boolean retrieve(OutputStream stream) throws IOException, LogFileStorageException;
}

/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

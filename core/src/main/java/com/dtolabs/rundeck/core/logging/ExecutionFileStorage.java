/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Handles storage and retrieval of typed files for an execution, the filetype is specified in the {@link #store(String,
 * java.io.InputStream, long, java.util.Date)} and {@link #retrieve(String, java.io.OutputStream)} methods, and more
 * than one filetype may be stored or retrieved for the same execution.
 */
public interface ExecutionFileStorage {
    /**
     * Stores a file of the given file type, read from the given stream
     *
     * @param filetype     filetype or extension of the file to store
     * @param stream       the input stream
     * @param length       the file length
     * @param lastModified the file modification time
     *
     * @return true if successful
     *
     * @throws java.io.IOException if an IO error occurs
     * @throws com.dtolabs.rundeck.core.logging.ExecutionFileStorageException if other errors occur
     */
    boolean store(String filetype, InputStream stream, long length, Date lastModified) throws IOException,
            ExecutionFileStorageException;

    /**
     * Write a file of the given file type to the given stream
     *
     * @param filetype key to identify stored file
     * @param stream   the output stream
     *
     * @return true if successful
     *
     * @throws IOException if an IO error occurs
     * @throws com.dtolabs.rundeck.core.logging.ExecutionFileStorageException if other errors occur
     */
    boolean retrieve(String filetype, OutputStream stream) throws IOException, ExecutionFileStorageException;
}

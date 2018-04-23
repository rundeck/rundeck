/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A ResourceModelSource that can write formatted model data
 *
 * @author greg
 * @since 9/5/17
 */
public interface WriteableModelSource {
    /**
     * @return the mime type of the data
     */
    String getSyntaxMimeType();

    /**
     * @return optional description of the source
     */
    default String getSourceDescription() {
        return null;
    }

    /**
     * read current data into the sink
     *
     * @param sink
     *
     * @return bytes written
     */
    long readData(OutputStream sink) throws IOException, ResourceModelSourceException;

    /**
     * @return true if the call to {@link #readData(OutputStream)} is expected to succeed.
     */
    boolean hasData();

    /**
     * Write new data in the expected format
     *
     * @param data data
     *
     * @return bytes written
     * @throws IOException if an IO error occurs
     * @throws ResourceModelSourceException if the data is not valid
     */
    long writeData(InputStream data) throws IOException, ResourceModelSourceException;
}

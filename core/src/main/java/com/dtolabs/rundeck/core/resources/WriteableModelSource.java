/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
 * A ResourceModelSource that store formatted model data
 *
 * @author greg
 * @since 9/5/17
 */
public interface WriteableModelSource {
    /**
     * @return the file format expected
     */
    String getFormat();

    /**
     * read current data into the sink
     *
     * @param sink
     *
     * @return bytes written
     */
    long readData(OutputStream sink) throws IOException;

    /**
     * Write new data in the expected format
     *
     * @param data data
     *
     * @return bytes written
     */
    long writeData(InputStream data) throws IOException;
}

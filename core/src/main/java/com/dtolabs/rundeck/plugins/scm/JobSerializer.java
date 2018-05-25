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

package com.dtolabs.rundeck.plugins.scm;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Can serialize a job to an outputstream in a format
 */
public interface JobSerializer {

    /**
     * @param format       format name: 'xml' or 'yaml'
     * @param outputStream destination
     */
    void serialize(String format, OutputStream outputStream) throws IOException;

    /**
     * @param format       format name: 'xml' or 'yaml'
     * @param outputStream destination
     * @param preserveUuid if true, preserve UUID in output, otherwise remove it
     * @param sourceId     if present, and preserveUuid is false, substitute the sourceId in place of the ID
     */
    void serialize(String format, OutputStream outputStream, boolean preserveUuid, String sourceId) throws IOException;
}

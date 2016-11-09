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

import java.io.InputStream;
import java.util.Map;

/**
 * Can import a job
 */
public interface JobImporter {
    /**
     * Import a serialized job, preserving the UUID
     *
     * @param format         format, 'xml' or 'yaml'
     * @param input          input stream
     * @param importMetadata metadata to attach to the job
     *
     * @return result
     */
    ImportResult importFromStream(String format, InputStream input, Map importMetadata);

    /**
     * Import a Map-representation of a Job, preserving the UUID
     *
     * @param input          input map data
     * @param importMetadata metadata to attach to the job
     *
     * @return result
     */
    ImportResult importFromMap(Map input, Map importMetadata);

    /**
     * Import a Map-representation of a Job
     *
     * @param input          input map data
     * @param importMetadata metadata to attach to the job
     * @param preserveUuid   if true, preserve any UUID on import, otherwise remove it
     *
     * @return result
     */
    ImportResult importFromMap(Map input, Map importMetadata, boolean preserveUuid);

    /**
     * Import a serialized job
     *
     * @param format         format, 'xml' or 'yaml'
     * @param input          input stream
     * @param importMetadata metadata to attach to the job
     * @param preserveUuid   if true, preserve any UUID on import, otherwise remove it
     *
     * @return result
     */
    ImportResult importFromStream(String format, InputStream input, Map importMetadata, boolean preserveUuid);
}

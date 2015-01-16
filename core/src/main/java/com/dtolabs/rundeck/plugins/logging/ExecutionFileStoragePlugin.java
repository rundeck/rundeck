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

package com.dtolabs.rundeck.plugins.logging;

import com.dtolabs.rundeck.core.logging.ExecutionFileStorage;
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException;

import java.util.Map;

/**
 * Plugin to implement {@link com.dtolabs.rundeck.core.logging.ExecutionFileStorage}
 */
public interface ExecutionFileStoragePlugin extends ExecutionFileStorage {
    /**
     * Initializes the plugin with contextual data
     *
     * @param context context data
     */
    public void initialize(Map<String, ? extends Object> context);

    /**
     * Returns true if the file for the context and the given filetype is available, false otherwise
     *
     * @param filetype file type or extension of the file to check
     *
     * @return true if a file with the given filetype is available for the context
     *
     * @throws com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
     *          if there is an error determining the availability
     */
    public boolean isAvailable(String filetype) throws ExecutionFileStorageException;

}

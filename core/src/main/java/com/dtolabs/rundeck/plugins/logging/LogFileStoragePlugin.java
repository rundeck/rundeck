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

import com.dtolabs.rundeck.core.logging.LogFileStorage;
import com.dtolabs.rundeck.core.logging.LogFileStorageException;

import java.util.Map;

/**
 * Plugin interface for Log file storage
 * @deprecated no longer used, replaced by {@link ExecutionFileStoragePlugin}
 */
public interface LogFileStoragePlugin extends LogFileStorage {
    /**
     * Initializes the plugin with contextual data
     * @param context context data
     */
    public void initialize(Map<String, ? extends Object> context);

    /**
     * Returns true if the file is available, false otherwise
     * @return true if available
     * @throws LogFileStorageException if there is an error determining the availability
     */
    public boolean isAvailable() throws LogFileStorageException;
}

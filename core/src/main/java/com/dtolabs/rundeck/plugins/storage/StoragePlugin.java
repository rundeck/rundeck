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

package com.dtolabs.rundeck.plugins.storage;

import com.dtolabs.rundeck.core.storage.StorageTree;
import org.rundeck.storage.api.Path;

/**
 * Plugin to provide a storage backend.
 */
public interface StoragePlugin extends StorageTree {

    /**
     * Returns true if the plugin is configured and ready to be used and false otherwise
     */
    default boolean isConfigured(Path basePath) {
        return listDirectory(basePath) != null;
    }
}

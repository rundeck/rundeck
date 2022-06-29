/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.plugins.nodes;

import com.dtolabs.rundeck.core.common.INodeEntry;

/**
 * Plugin interface to modify node entries from sources
 */
public interface NodeEnhancerPlugin {
    /**
     * Update a node
     *
     * @param project project name
     * @param node     node
     */
    void updateNode(String project, IModifiableNodeEntry node);

    /**
     * It allows the node enhancer to indicate that it should be skipped
     * @param projectName project name
     * @return boolean indicates whether this enhancer should be skipped
     */
    default boolean shouldSkip(String projectName){
        return false;
    }
}

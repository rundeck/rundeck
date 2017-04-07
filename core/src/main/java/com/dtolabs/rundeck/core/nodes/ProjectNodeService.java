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

package com.dtolabs.rundeck.core.nodes;

import com.dtolabs.rundeck.core.common.INodeSet;

/**
 * Service for interact with nodes.
 */
public interface ProjectNodeService {


    /**
     * Expire cache and refresh node list
     *
     * @param project Project name
     */
    void refreshProjectNodes(final String project);

    /**
     * Get the current nodes
     * @param name Project name
     * @return nodes
     */
    INodeSet getNodeSet(final String name);
}

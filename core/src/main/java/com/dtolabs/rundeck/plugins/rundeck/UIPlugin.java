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

package com.dtolabs.rundeck.plugins.rundeck;

import java.util.List;

/**
 *
 */
public interface UIPlugin {
    /**
     * @param path
     *
     * @return true if this plugin applies at the path
     */
    boolean doesApply(String path);

    /**
     *
     * @param path
     * @return list of resources available at the path
     */
    List<String> resourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of javascript resources to load at the path
     */
    List<String> scriptResourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of css stylesheets to load at the path
     */
    List<String> styleResourcesForPath(String path);

    /**
     * @param path
     *
     * @return list of plugin names this plugin depends on for the specified path
     */
    List<String> requires(String path);
}

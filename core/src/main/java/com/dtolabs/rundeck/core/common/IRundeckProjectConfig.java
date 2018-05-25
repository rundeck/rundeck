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

package com.dtolabs.rundeck.core.common;

import java.util.Date;
import java.util.Map;

/**
 * definition of a project's configuration
 */
public interface IRundeckProjectConfig {
    /**
     * @return project name
     */
    public String getName();
    /**
     * @param name property name
     *
     * @return the property value by name
     */
    String getProperty(String name);

    /**
     *
     * @param key property name
     * @return true if present, false otherwise
     */
    boolean hasProperty(String key);

    /**
     * @return the merged properties available for the project
     */
    Map<String,String> getProperties();

    /**
     * @return the direct properties set for the project
     */
    Map<String,String> getProjectProperties();
    /**
     * @return last modified time for configuration in epoch time
     */
    Date getConfigLastModifiedTime();
}

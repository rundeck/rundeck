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

import java.util.Properties;
import java.util.Set;

/**
 * Created by greg on 2/2/16.
 */
public interface IRundeckProjectConfigModifier {

    /**
     * Update the project properties file by setting updating the given properties, and removing
     * any properties that have a prefix in the removePrefixes set
     *
     * @param properties     new properties to put in the file
     * @param removePrefixes prefixes of properties to remove from the file
     */
    void mergeProjectProperties(Properties properties, Set<String> removePrefixes);

    /**
     * Set the project properties file contents exactly
     *
     * @param properties new properties to use in the file
     */
    void setProjectProperties(Properties properties);

    void generateProjectPropertiesFile(boolean overwrite, Properties properties, boolean addDefault);
}

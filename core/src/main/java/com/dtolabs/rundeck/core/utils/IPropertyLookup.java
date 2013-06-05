/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.common.PropertyRetriever;

import java.util.Map;

/**
 * simple interface to lookup property data
 */
public interface IPropertyLookup extends PropertyRetriever{
    /**
     * get property value
     *
     * @param key the name of the property
     * @return property value
     */
    String getProperty(String key);

    /**
     * checks if property value exists
     *
     * @param key name of the property
     * @return true if it exists; false otherwise
     */
    boolean hasProperty(String key);

    /**
     * Retrieves map of property data
     *
     * @return {@link Map} containing property key/value pair
     * @throws PropertyLookupException thrown if loaderror
     */
    Map getPropertiesMap();
}

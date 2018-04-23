/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.logs

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin

/**
 * @author greg
 * @since 5/5/17
 */
@Plugin(name = PropertiesConverterPlugin.PROVIDER_NAME, service = 'ContentConverter')
@PluginDescription(title = 'Properties Data Converter',
        description = '''Parses Java style Properties data into a Java object.

The "HTML Table View Converter" plugin can render this as a HTML Table.''')
class PropertiesConverterPlugin implements ContentConverterPlugin {
    public static final String PROVIDER_NAME = 'properties-data-view'

    /**
     * @param clazz    input object type
     * @param dataType input data type string
     *
     * @return true if the class and datatype are supported
     */
    boolean isSupportsDataType(Class<?> clazz, String dataType) {
        clazz == String && dataType == 'application/x-java-properties'
    }

    /**
     * @param clazz input object type
     * @param dataType input data type string
     *
     * @return output object type
     */
    Class<?> getOutputClassForDataType(Class<?> clazz, String dataType) {
        isSupportsDataType(clazz, dataType) ? List : null
    }

    /**
     * @param clazz input object type
     * @param dataType input data type string
     *
     * @return output data type string
     */
    String getOutputDataTypeForContentDataType(Class<?> clazz, String dataType) {
        isSupportsDataType(clazz, dataType) ? 'application/x-java-map-or-list' : null
    }

    /**
     * Convert the input object
     *
     * @param data input object
     * @param dataType input data type string
     *
     * @return output object
     */
    Object convert(Object data, String dataType, Map<String, String> metadata) {
        if (!isSupportsDataType(data.getClass(), dataType)) {
            return null
        }
        Properties props = new Properties()
        try {
            props.load(new StringReader((String) data))
            return props
        } catch (IOException exc) {

        }
        return null
    }
}
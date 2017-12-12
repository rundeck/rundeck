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

package com.dtolabs.rundeck.server.plugins.logs

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Converts JSON string into a java Object with data type 'application/x-java-map-or-list', which
 * can be rendered to HTML via the {@link HTMLTableViewConverterPlugin}
 * @author greg
 * @since 5/5/17
 */
@Plugin(name = JsonConverterPlugin.PROVIDER_NAME, service = 'ContentConverter')
@PluginDescription(title = 'JSON Data Converter',
        description = '''Parses JSON text into a a Java object.\n\nThe "HTML Table View Converter" plugin can render 
this as a HTML Table.''')
class JsonConverterPlugin implements ContentConverterPlugin {

    /*
     * @param clazz    input object type
     * @param dataType input data type string
     *
     * @return true if the class and datatype are supported
     */


    public static final String PROVIDER_NAME = 'json-data-view'

    boolean isSupportsDataType(Class<?> clazz, String dataType) {
        clazz == String && dataType == 'application/json'
    }

    /**
     * @param clazz input object type
     * @param dataType input data type string
     *
     * @return output object type
     */
    Class<?> getOutputClassForDataType(Class<?> clazz, String dataType) {

        clazz == String && dataType == 'application/json' ? List : null
    }

    /**
     * @param clazz input object type
     * @param dataType input data type string
     *
     * @return output data type string
     */
    String getOutputDataTypeForContentDataType(Class<?> clazz, String dataType) {

        clazz == String && dataType == 'application/json' ? 'application/x-java-map-or-list' : null
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
        if (!(data.getClass() == String && dataType == 'application/json')) {
            return null
        }
        def mapper = new ObjectMapper()
        mapper.readValue((String) data, Object)
    }
}

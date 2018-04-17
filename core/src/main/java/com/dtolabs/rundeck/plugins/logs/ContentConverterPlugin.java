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

package com.dtolabs.rundeck.plugins.logs;

import java.util.Map;

/**
 * Plugin type which can convert log and other data into a view type:
 * allows converting a String or object to another object type, given a "data type" string, such as "text/plain" or
 * "application/json".
 * To convert, e.g. a json string into an HTML table for display in a log, you
 * would support an input object of type {@link String} and a data type string of "application/json",
 * and you would produce an object of type {@link String} and a data type string of "text/html"
 *
 * @author greg
 * @since 5/5/17
 */
public interface ContentConverterPlugin {
    /**
     * @param clazz    input object type
     * @param dataType input data type string
     *
     * @return true if the class and datatype are supported
     */
    boolean isSupportsDataType(Class<?> clazz, String dataType);

    /**
     * @param clazz    input object type
     * @param dataType input data type string
     *
     * @return output object type
     */
    Class<?> getOutputClassForDataType(Class<?> clazz, String dataType);

    /**
     * @param clazz    input object type
     * @param dataType input data type string
     *
     * @return output data type string
     */
    String getOutputDataTypeForContentDataType(Class<?> clazz, String dataType);

    /**
     * Convert the input object
     *
     * @param data     input object
     * @param dataType input data type string
     * @param metadata metadata about the content
     *
     * @return output object
     */
    Object convert(Object data, String dataType, Map<String,String> metadata);
}

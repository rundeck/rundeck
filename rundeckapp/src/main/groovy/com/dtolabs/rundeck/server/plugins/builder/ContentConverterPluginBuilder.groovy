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

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger

class ContentConverterPluginBuilder extends ScriptPluginBuilder implements PluginBuilder<ContentConverterPlugin> {
    static Logger logger = Logger.getLogger(StreamingLogWriterPluginBuilder)
    Map<String, Closure> handlers = [:]
    List<Converter> converters = []

    static class Converter {
        DataType input
        DataType output
        Closure converter

        boolean acceptsInput(DataType test) {
            return input.dataClass.isAssignableFrom(test.dataClass) && input.dataType == test.dataType
        }
    }

    static class DataType {
        String dataType
        Class<?> dataClass

        boolean equals(final o) {
            if (this.is(o)) {
                return true
            }
            if (getClass() != o.class) {
                return false
            }

            DataType dataType1 = (DataType) o

            if (dataClass != dataType1.dataClass) {
                return false
            }
            if (dataType != dataType1.dataType) {
                return false
            }

            return true
        }

        int hashCode() {
            int result
            result = (dataType != null ? dataType.hashCode() : 0)
            result = 31 * result + (dataClass != null ? dataClass.hashCode() : 0)
            return result
        }

        @Override
        public String toString() {
            return "DataType{" +
                   "dataType='" + dataType + '\'' +
                   ", class=" + dataClass +
                   '}';
        }
    }

    static DataType dataType(String type, Class<?> clazz) {
        return new DataType(dataClass: clazz, dataType: type)
    }

    static DataType dataType(String type) {
        return new DataType(dataClass: String, dataType: type)
    }

    static DataType dataType(Class<?> clazz, String type) {
        return new DataType(dataClass: clazz, dataType: type)
    }

    ContentConverterPluginBuilder(Class clazz, String name) {
        super(clazz, name)
    }

    @Override
    ContentConverterPlugin buildPlugin() {
        return new ScriptContentConverterPlugin(converters, descriptionBuilder.build())
    }
    /**
     * Converter DSL, converts the input data type to text/html with the closure
     * @param input input data type
     * @param clos
     */
    void convert(input, Closure clos) {
        convert input, dataType('text/html'), clos
    }
    /**
     * Converter DSL, converts the input data type to the output datatype with the closure
     * @param input input data type
     * @param output output data type
     * @param clos
     */
    void convert(input, output, Closure clos) {
        if (input instanceof String) {
            input = dataType(input)
        } else if (!(input instanceof DataType)) {
            throw new IllegalArgumentException(
                "Invalid input type: ${input.class}, expected a String, or a DataType"
            );
        }
        if (output instanceof String) {
            output = dataType(output)
        } else if (!(output instanceof DataType)) {
            throw new IllegalArgumentException(
                "Invalid output type: ${output.class}, expected a String, or a DataType"
            );
        }
        if (!ScriptContentConverterPlugin.validConverterClosure(clos)) {
            logger.error("Invalid converter closure for datatype: ${input} -> ${output}, expected 1, 2, or 3 params")
            throw new IllegalArgumentException(
                "Invalid converter closure for datatype: ${input} -> ${output}, expected 1, 2, or 3 params"
            );
        }

        converters << new Converter(input: input, output: output, converter: clos)
    }
}

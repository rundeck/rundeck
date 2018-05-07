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

import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import org.apache.log4j.Logger

class ScriptContentConverterPlugin implements ContentConverterPlugin, Describable, Configurable {
    static Logger logger = Logger.getLogger(ScriptLogFilterPlugin)
    Description description
    private List<ContentConverterPluginBuilder.Converter> converters
    Map configuration

    ScriptContentConverterPlugin(List<ContentConverterPluginBuilder.Converter> converters, Description description) {
        this.description = description
        this.converters = converters
        this.configuration = [:]
    }

    @Override
    void configure(final Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }

    @Override
    boolean isSupportsDataType(final Class<?> clazz, final String dataType) {
        def dt = new ContentConverterPluginBuilder.DataType(dataType: dataType, dataClass: clazz)
        findConverter(dt)
    }

    @Override
    Class<?> getOutputClassForDataType(final Class<?> clazz, final String dataType) {
        def dt = new ContentConverterPluginBuilder.DataType(dataType: dataType, dataClass: clazz)
        findConverter(dt)?.output?.dataClass
    }

    public ContentConverterPluginBuilder.Converter findConverter(ContentConverterPluginBuilder.DataType dt) {
        converters.find { it.acceptsInput(dt)}
    }

    @Override
    String getOutputDataTypeForContentDataType(final Class<?> clazz, final String dataType) {
        def dt = new ContentConverterPluginBuilder.DataType(dataType: dataType, dataClass: clazz)
        findConverter(dt)?.output?.dataType
    }

    @Override
    Object convert(final Object data, final String dataType, final Map<String, String> metadata) {
        def dt = new ContentConverterPluginBuilder.DataType(dataType: dataType, dataClass: data.class)
        def cv = findConverter(dt)
        if (!cv) {
            return null
        }
        def Closure newclos = cv.converter.clone()
        newclos.delegate = [metadata: metadata, data: data, dataType: dataType, configuration: configuration]
        newclos.resolveStrategy = Closure.DELEGATE_ONLY
        def output
        if (newclos.getMaximumNumberOfParameters() == 3) {
            output = newclos.call(data, dataType, metadata)
        } else if (newclos.getMaximumNumberOfParameters() == 2) {
            output = newclos.call(data, metadata)
        } else {
            output = newclos.call(data)
        }
        if (cv.output.dataClass == String && output instanceof GString) {
            //convert via toString
            output = output.toString()
        }
//        println("convert${dt}-> " + output)
        output
    }

    static boolean validConverterClosure(final Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes == [Object, String, Map].toArray()
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes == [Object, Map].toArray()
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes == [Object].toArray()
        }
        false
    }

}

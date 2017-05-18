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

/**
 * @author greg
 * @since 5/8/17
 */

@Plugin(name = HTMLViewConverterPlugin.PROVIDER_NAME, service = 'ContentConverter')
@PluginDescription(title = 'HTML View Converter',
        description = '''Allows HTML embedded in log data to be rendered.

Note: this plugin performs no changes, but passes HTML to be sanitized and rendered.''')
class HTMLViewConverterPlugin implements ContentConverterPlugin {
    public static final String PROVIDER_NAME = 'html-data-view-converter'

    @Override
    boolean isSupportsDataType(final Class<?> clazz, final String dataType) {
        clazz == String && dataType == 'text/html'
    }

    @Override
    Class<?> getOutputClassForDataType(final Class<?> clazz, final String dataType) {
        isSupportsDataType(clazz, dataType) ? String : null
    }

    @Override
    String getOutputDataTypeForContentDataType(final Class<?> clazz, final String dataType) {
        return isSupportsDataType(clazz, dataType) ? 'text/html' : null
    }

    @Override
    Object convert(final Object data, final String dataType, final Map<String, String> metadata) {
        data
    }
}

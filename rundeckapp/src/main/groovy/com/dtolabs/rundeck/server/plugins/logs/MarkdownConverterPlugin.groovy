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

@Plugin(name = MarkdownConverterPlugin.PROVIDER_NAME, service = 'ContentConverter')
@PluginDescription(title = 'Markdown View Converter',
        description = 'Renders Markdown as HTML')
class MarkdownConverterPlugin implements ContentConverterPlugin {
    public static final String PROVIDER_NAME = 'markdown-data-view'
    public static final String MARKDOWN_DATA_TYPE = 'text/x-markdown'

    @Override
    boolean isSupportsDataType(final Class<?> clazz, final String dataType) {
        clazz == String && MARKDOWN_DATA_TYPE == dataType
    }

    @Override
    Class<?> getOutputClassForDataType(final Class<?> clazz, final String dataType) {
        isSupportsDataType(clazz, dataType) ? String : null
    }

    @Override
    String getOutputDataTypeForContentDataType(final Class<?> clazz, final String dataType) {
        isSupportsDataType(clazz, dataType) ? 'text/html' : null
    }

    @Override
    Object convert(final Object data, final String dataType, final Map<String, String> metadata) {
        if (!isSupportsDataType(data.getClass(), dataType)) {
            return null
        }
        ((String) data).decodeMarkdown()
    }
}

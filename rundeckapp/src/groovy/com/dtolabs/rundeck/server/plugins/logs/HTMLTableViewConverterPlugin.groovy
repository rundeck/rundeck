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
 * Renders a java List or Map as HTML
 * @author greg
 * @since 5/5/17
 */
@Plugin(name = HTMLTableViewConverterPlugin.PROVIDER_NAME, service = 'ContentDataTypeViewPlugin')
@PluginDescription(title = 'HTML Table View Converter',
        description = '''Renders structured data as a Table in HTML. The input should be a List or Map. If the List 
contains Maps, the first item\'s keys will be the table headers.''')
class HTMLTableViewConverterPlugin implements ContentConverterPlugin {
    static final Map<String, Class<?>> datatypes = [
            (MAP_LIST_TYPE): List,
            (MAP_TYPE)     : Map,
            (LIST_TYPE)    : List,
    ]
    static final List<Class<?>> mapOrListTypes = [List, Map]
    public static final String PROVIDER_NAME = 'log-data-table-view'
    public static final String MAP_LIST_TYPE = 'application/x-java-map-list'
    public static final String MAP_TYPE = 'application/x-java-map'
    public static final String LIST_TYPE = 'application/x-java-list'
    public static final String MAP_OR_LIST_TYPE = 'application/x-java-map-or-list'

    @Override
    boolean isSupportsDataType(final Class<?> clazz, final String dataType) {
        datatypes[dataType]?.isAssignableFrom(clazz) ||
                (dataType == MAP_OR_LIST_TYPE && mapOrListTypes.find { it.isAssignableFrom(clazz) })
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
    Object convert(Object data, String dataType, Map<String, String> metadata) {
        if (data == null || !dataType || !(isSupportsDataType(data.getClass(), dataType))) {
            return null;
        }
        //render data as a Table.
        if (Map.isAssignableFrom(data.getClass())) {
            return renderSimpleMap((Map) data, metadata)
        } else {
            return renderSimpleList((List) data, metadata)
        }
    }

    public static String renderSimpleList(List list, Map<String, String> metadata) {
        def first = list.first()
        if (first instanceof Map) {
            return renderTableHtml(list, metadata)
        }
        def out = new StringBuffer()
        openTag(out, 'ol')
        list.each { item ->
            openTag(out, 'li')
            if (item instanceof List) {
                out << renderSimpleList(item, metadata)
            } else if (item instanceof Map) {
                out << renderSimpleMap(item, metadata)
            } else {
                out << item.toString()
            }
            closeTag(out, 'li')
        }
        closeTag(out, 'ol')
        return out.toString()
    }

    public static String renderSimpleMap(Map map, Map<String, String> metadata) {
        def out = new StringBuffer()
        renderTableStart(out, metadata['css-class'])
        if (metadata['table-title']) {
            renderTableTitle(out, metadata['table-title'], 2)
        }
        renderTableHeaders(out, ['Key', 'Value'])
        map.each { key, item ->
            openTag(out, 'tr')
            openTag(out, 'td')
            out << key
            closeTag(out, 'td')
            openTag(out, 'td')
            if (item instanceof List) {
                out << renderSimpleList(item, metadata)
            } else if (item instanceof Map) {
                out << renderSimpleMap(item, metadata)
            } else {
                out << item.toString()
            }
            closeTag(out, 'td')
            closeTag(out, 'tr')
        }
        closeTag(out, 'table')
        return out.toString()
    }

    private static StringBuffer openTag(StringBuffer out, String tag) {
        out << '<' + tag + '>'
    }

    private static StringBuffer closeTag(StringBuffer out, String tag) {
        out << '</' + tag + '>\n'
    }

    private static StringBuffer renderTableStart(StringBuffer out, css = null) {
        out << '<table class="table table-condensed table-bordered table-embed table-data-embed ' + (css ?: '') + '">\n'
    }

    public static String renderTableHtml(List<Map<String, ?>> list, Map<String, String> metadata) {
        def keys = list?.first()?.keySet()?.sort()
        if (!keys) {
            return null
        }
        def out = new StringBuffer()
        renderTableStart(out, metadata['css-class'])
        if (metadata['table-title']) {
            renderTableTitle(out, metadata['table-title'], keys.size())
        }
        renderTableHeaders(out, keys)
        list.each { dataObj ->


            openTag(out, 'tr')
            keys.each { key ->
                def item = dataObj[key]

                closeTag(out, 'td')
                if (item instanceof List) {
                    out << renderSimpleList(item, metadata)
                } else if (item instanceof Map) {
                    out << renderSimpleMap(item, metadata)
                } else {
                    out << item.toString()
                }
                closeTag(out, 'td')
            }
            closeTag(out, 'tr')
        }
        closeTag(out, 'table')
        return out.toString()
    }

    private static void renderTableHeaders(out, List<String> headerNames) {
        openTag(out, 'tr')
        headerNames.each { key ->
            out << '<th class="table-header">' + key + '</th>'
        }
        closeTag(out, 'tr')
    }

    private static void renderTableTitle(out, String title, int colspan) {
        openTag(out, "tr")
        openTag(out, "th colspan=\"$colspan\" class=\"table-header\"")
        out << title
        closeTag(out, 'th')
        closeTag(out, 'tr')
    }
}

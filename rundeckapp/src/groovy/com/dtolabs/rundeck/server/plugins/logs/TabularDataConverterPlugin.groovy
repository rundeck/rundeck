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

import java.util.regex.Pattern

/**
 * @author greg
 * @since 5/8/17
 */

@Plugin(name = TabularDataConverterPlugin.PROVIDER_NAME, service = 'ContentConverter')
@PluginDescription(title = 'Tabular Data Converter', description = '''Parses Tabular text (csv) into a a Java object.

The "HTML Table View Converter" plugin can render this as a HTML Table.

Expected content data type: `text/csv`.

The default separator is a comma, but another separator can be specified with the `sep` parameter:

* `space` - space character
* `tab` - tab character
* `comma` - comma character
* `vertbar` - vertical bar character (`|`)
* `<anything>` - any string

If the `header` parameter is `true`, Or if the second row of data is all `-` (hyphen) characters, the first row of 
data will be used as the header names. 

Additionally if the data type contains `;header=present` a header is assumed present.
'''
)
class TabularDataConverterPlugin implements ContentConverterPlugin {
    public static final String PROVIDER_NAME = 'tabular-data-view'
    public static final String TYPE_CSV = 'text/csv'
    public static final String META_SEPARATOR_KEY = 'sep'
    public static final String META_HEADER_KEY = 'header'
    public static final Map<String, String> SEPARATORS = [comma: ',', space: ' ', tab: '\t', vertbar: '|']

    @Override
    boolean isSupportsDataType(final Class<?> clazz, final String dataType) {
        clazz == String && dataType.startsWith(TYPE_CSV)
    }

    @Override
    Class<?> getOutputClassForDataType(final Class<?> clazz, final String dataType) {
        isSupportsDataType(clazz, dataType) ? List : null
    }

    @Override
    String getOutputDataTypeForContentDataType(final Class<?> clazz, final String dataType) {
        isSupportsDataType(clazz, dataType) ? HTMLTableViewConverterPlugin.COL_LIST_TYPE : null
    }

    @Override
    Object convert(final Object data, final String dataType, final Map<String, String> metadata) {
        //parse tab/comma separated values
        List<String> content = ((String) data).readLines()
        if (content.isEmpty()) {
            return null
        }
        String separator = ','//default
        if (metadata[META_SEPARATOR_KEY]) {
            separator = SEPARATORS[metadata[META_SEPARATOR_KEY]] ?: metadata[META_SEPARATOR_KEY]
        }
        def quotedSep = Pattern.quote(separator)
        boolean hasHeader = metadata[META_HEADER_KEY] == 'true'

        if (dataType.indexOf(";header=present") > 0) {
            hasHeader = true
        }

        List<List> result = []
        while(content.size()>0 && !content.first().trim()) {
            content.remove(0)
        }
        List first = content.remove(0).split(quotedSep, -1)
        if (content.size() > 0 && content.first().matches('^-+$')) {
            hasHeader = true
            content.remove(0)
        }
        List keys = null
        result << first
        int max = 0
        content.each {
            result << (it.split(quotedSep, -1) as List)
            max = Math.max(max, result[-1].size())
        }
        if (!hasHeader) {
            //generate header
            keys = []
            (0..<max).each {
                keys << colName(it)
            }
            result.add(0, keys)
        }


        return result
    }
    static final int ASCIIA = (int) 'A'.charAt(0)

    /**
     * Generate column names A,B,C...AA,BB,CC,...
     * @param i
     * @return
     */
    static String colName(int i) {
        int mult = 1 + (i > 0 ? i.intdiv(26) : 0)
        int index = i % 26

        ((char) (index + ASCIIA)).toString() * (mult)
    }

    static Map make(List data, List keys = null) {
        Map map = [:]
        data.eachWithIndex { d, i ->
            map[keys ? keys[i] : colName(i)] = d
        }
        map
    }
}

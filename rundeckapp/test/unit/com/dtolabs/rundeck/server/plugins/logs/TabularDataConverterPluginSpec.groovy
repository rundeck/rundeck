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

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/26/17
 */
class TabularDataConverterPluginSpec extends Specification {
    def "supported type"() {
        given:
        def plugin = new TabularDataConverterPlugin()
        expect:
        plugin.isSupportsDataType(String, 'text/csv')
        plugin.isSupportsDataType(String, 'text/csv; charset=UTF-8')

        plugin.getOutputDataTypeForContentDataType(String, 'text/csv') == HTMLTableViewConverterPlugin.MAP_LIST_TYPE
        plugin.getOutputClassForDataType(String, 'text/csv') == List
    }

    def "convert without header"() {
        given:
        def plugin = new TabularDataConverterPlugin()
        def type = 'text/csv'
        def meta = [:]
        when:

        def result = plugin.convert(data, type, meta)
        then:
        result == expected

        where:
        data                 | expected
        'a,b,c\nd,e,f'       | [[A: 'a', B: 'b', C: 'c'], [A: 'd', B: 'e', C: 'f']]
        'a,b,c\nd,e,f\n'     | [[A: 'a', B: 'b', C: 'c'], [A: 'd', B: 'e', C: 'f']]
        '\n\na,b,c\nd,e,f\n' | [[A: 'a', B: 'b', C: 'c'], [A: 'd', B: 'e', C: 'f']]
    }

    @Unroll
    def "convert with header"() {
        given:
        def plugin = new TabularDataConverterPlugin()
        when:

        def result = plugin.convert(data, type, meta)
        then:
        result == [['1': 'a', '2': 'b', '3': 'c'], ['1': 'd', '2': 'e', '3': 'f']]

        where:
        data                           | type                      | meta
        '1,2,3\n---\na,b,c\nd,e,f'     | 'text/csv'                | [:]
        '1,2,3\na,b,c\nd,e,f'          | 'text/csv;header=present' | [:]
        '1,2,3\na,b,c\nd,e,f'          | 'text/csv'                | [header: 'true']
        '\n\n1,2,3\n---\na,b,c\nd,e,f' | 'text/csv'                | [:]
        '\n\n1,2,3\na,b,c\nd,e,f'      | 'text/csv;header=present' | [:]
        '\n\n1,2,3\na,b,c\nd,e,f'      | 'text/csv'                | [header: 'true']
    }
}

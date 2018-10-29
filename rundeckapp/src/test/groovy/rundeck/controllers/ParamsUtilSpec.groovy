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

package rundeck.controllers

import spock.lang.Specification
import spock.lang.Unroll

class ParamsUtilSpec extends Specification {
    @Unroll
    def "cleanMap"() {
        given:
        when:
            def result = ParamsUtil.cleanMap(input)
        then:
            result == expect
        where:
            input                                                                                                  || expect
            [:]                                                                                                    || [:]
            [a: 'b', c: '']                                                                                        || [a: 'b']
            [a: 'b', c: null]                                                                                      || [a: 'b']
            ['z._type': 'embedded', 'z.config.x': 'y', 'z.config.t': 'u']                                          || [z: [x: 'y', t: 'u']]
            [optionData: [_type: "map"], "optionData._type": "map", jobId: "0226f979-9ef3-485d-a916-faa1a8f9791e"] || [optionData: [_type:['map', 'map']], jobId: "0226f979-9ef3-485d-a916-faa1a8f9791e"]
            ["optionData.map.0.key"  : "bwapba",
             optionData              :
                 ["map.0.key"  : "bwapba",
                  map          : [
                      "0.key"  : "bwapba",
                      "0"      : [
                          "key": "bwapba",
                          value: "fff"],
                      "1.value": "2018-02-21T12:36:33-08:00",
                      "1"      : [
                          value: "2018-02-21T12:36:33-08:00",
                          key  : "date1"
                      ],
                      "0.value": "fff",
                      "1.key"  : "date1"
                  ],
                  "map.1.value": "2018-02-21T12:36:33-08:00",
                  "_type"      : "map",
                  "map.0.value": "fff",
                  "map.1.key"  : "date1"
                 ],
             "optionData.map.1.value": "2018-02-21T12:36:33-08:00",
             "optionData._type"      : "map",
             jobId                   : "4cd55607-ffd7-4db2-a622-d5446b996c7e",
             "optionData.map.0.value": "fff",
             "optionData.map.1.key"  : "date1"
            ]                                                                                                      || [jobId: "4cd55607-ffd7-4db2-a622-d5446b996c7e", optionData: [bwapba: "fff", date1: "2018-02-21T12:36:33-08:00"]]
            [
                "actions._indexes"                                           : "dbd3da9c_1",
                "actions._type"                                              : "list",
                "actions.entry[dbd3da9c_1]._type"                            : "embeddedPlugin",
                "actions.entry[dbd3da9c_1].type"                             : "testaction1",
                "actions.entry[dbd3da9c_1].config.actions._type"             : "embedded",
                "actions.entry[dbd3da9c_1].config.actions.config.stringvalue": "asdf",
                "actions.entry[dbd3da9c_1].config.actions"                   : "blahblah",]                        ||
            [actions: [[type: 'testaction1', config: [actions: [stringvalue: 'asdf']]]]]

    }

    def "cleanMap removes underscore keys"() {
        given:
        when:
            def result = ParamsUtil.cleanMap(input)
        then:
            result == expect
        where:
            input                 || expect
            [_a: 'b']             || [:]
            ['_somedata': 'blah'] || [:]

    }

    @Unroll
    def "parseMapTypeEntries"() {
        given:
        when:
            def result = ParamsUtil.parseMapTypeEntries(input)
        then:
            result == expect
        where:
            input                                                    || expect
            [:]                                                      || [:]
            ['_type': 'map']                                         || [:]
            ['_type': 'map', 'map': 'x']                             || [:]
            ['_type': 'map', 'map': [:]]                             || [:]
            ['_type': 'map', 'map': [:], 'extra': 'asdf']            || [:]
            ['_type': 'zmap', 'map': [:]]                            || [:]
            ['_type': 'map', 'map': ['0.key': 'z', '0.value': 'x']]  || [z: 'x']
            [_type: 'map', 'map': ['0.key': 'z', '0.value': 'x']]    || [z: 'x']
            [_type: 'map', 'map': ['0': ['key': 'z', 'value': 'p']]] || [z: 'p']
    }

    @Unroll
    def "parseEmbeddedTypeEntries"() {
        given:
        when:
            def result = ParamsUtil.parseEmbeddedTypeEntries(input)
        then:
            result == expect
        where:
            input                                                         || expect
            [:]                                                           || [:]
            ['z._type': 'embedded', 'z.config.x': 'y', 'z.config.t': 'u'] || [:]
            ['_type': 'embedded', config: ['x': 'y', 't': 'u']]           || [x: 'y', t: 'u']
            ['_type': 'embedded', 'config.x': 'y', 'config.t': 'u']       || [:]
    }

    @Unroll
    def "parseEmbeddedPluginEntries"() {
        given:
        when:
            def result = ParamsUtil.parseEmbeddedPluginEntries(input)
        then:
            result == expect
        where:
            input                                                                                 || expect
            [:]                                                                                   || [:]
            ['z._type': 'embeddedPlugin', 'z.config.x': 'y', 'z.config.t': 'u', 'z.type': 'asdf'] || [:]
            ['_type': 'embeddedPlugin', 'config.x': 'y', 'config.t': 'u', 'type': 'asdf']         || [:]
            ['_type': 'embeddedPlugin', 'config': ['x': 'y', 't': 'u'], 'type': 'asdf']           || [type: 'asdf', config: [x: 'y', t: 'u']]
    }


    @Unroll
    def "parseIndexedMap"() {
        given:

        when:
            def result = ParamsUtil.parseIndexedMapParams(input)
        then:
            result != null
            result == expect

        where:
            decomposed | input                                                                     || expect
            false      | ['0.key': 'abc', '0.value': 'xyz']                                         | [abc: 'xyz']
            true       | ['0': ['key': 'abc', 'value': 'xyz']]                                      | [abc: 'xyz']
            false      | ['0.key': 'abc', '0.value': '']                                            | [abc: '']
            true       | ['0': ['key': 'abc', 'value': '']]                                         | [abc: '']
            false      | ['0.key': 'abc']                                                           | [abc: '']
            true       | ['0': ['key': 'abc']]                                                      | [abc: '']
            false      | ['0.key': '', '0.value': 'xyz']                                            | [:]
            true       | ['0': [key: '', value: 'xyz']]                                             | [:]
            false      | ['blah': 'blee']                                                           | [:]
            false      | ['0.key': 'abc', '0.value': 'xyz', '1.key': 'def', '1.value': 'pqr']       |
            [abc: 'xyz', def: 'pqr']
            true       | ['0': ['key': 'abc', 'value': 'xyz'], '1': ['key': 'def', 'value': 'pqr']] |
            [abc: 'xyz', def: 'pqr']
            false      | ['0.key': '', '0.value': 'xyz', '1.key': 'def', '1.value': 'pqr']          | [def: 'pqr']
            false      | ['0.key': '', '1.key': 'def', '1.value': 'pqr']                            | [def: 'pqr']
            false      | ['0.key': 'abc', '0.value': 'xyz', '1.key': 'def', '2.value': 'pqr']       | [abc: 'xyz', def: '']
            false      | ['0.key': 'abc', '0.value': 'xyz', '2.key': 'def', '1.value': 'pqr']       | [abc: 'xyz']
            false      | ['0.key': 'abc', '0.value': 'xyz', 'blah': 'blee']                         | [abc: 'xyz']
            false      | ['0.key': 'abc', '0.value': 'xyz', '2.key': 'def', '2.value': 'pqr']       | [abc: 'xyz']

    }

    @Unroll
    def "parseMapList"() {
        given:

        when:
            def result = ParamsUtil.parseMapList(input)
        then:
            result != null
            result == expect

        where:
            input                                                                                                         |
            expect
            ['abc': 'xyz']                                                                                                |
            []
            ['_indexes': ['1', '2']]                                                                                      |
            []
            ['_indexes': ['1', '2'], 'entry[1]': [:]]                                                                     |
            []
            ['_indexes': ['1', '2'], 'entry[1]': [a: '']]                                                                 |
            [[a: '']]
            ['_indexes': ['1', '2'], 'entry[1]': [a: 'b']]                                                                |
            [[a: 'b']]
            ['_indexes': ['1', '2'], 'entry[1]': [a: 'b', c: ['_type': 'map', 'map': ['0.key': 'z', '0.value': 'w']]]]    |
            [[a: 'b', c: [z: 'w']]]
            ['_indexes': ['1', '2'], 'entry[1]': [a: 'b', c: ['_type': 'map', 'map': ['0': ['key': 'z', 'value': 'w']]]]] |
            [[a: 'b', c: [z: 'w']]]

    }

    @Unroll
    def "decomposeMap"() {

        expect:
            ParamsUtil.decomposeMap(input) == expected

        where:
            input                                                                  | expected
            ['a.b': 'c']                                                           | [a: ['b': 'c']]
            ['a.b': 'c', 'a.b.d': 'l']                                             | [a: ['b': [_value: 'c', 'd': 'l']]]
            ['a.b.d': 'l', 'a.b': 'c',]                                            | [a: ['b': [_value: 'c', 'd': 'l']]]
            [optionData: [_type: "map"], "optionData._type": "map", jobId: "123"] || [optionData: [_type:['map','map']], jobId: "123"]
            [optionData: [_type: "d"], "optionData._type": "map", jobId: "123"] || [optionData: [_type:['d','map']], jobId: "123"]

    }
}

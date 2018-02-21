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

class ParamsUtilSpec extends Specification {
    def "cleanMap"() {
        given:
        when:
        def result = ParamsUtil.cleanMap(input)
        then:
        result == expect
        where:
        input             || expect
        [:]               || [:]
        [a: 'b', c: '']   || [a: 'b']
        [a: 'b', c: null] || [a: 'b']
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
        ]                 || [jobId: "4cd55607-ffd7-4db2-a622-d5446b996c7e", optionData: [bwapba: "fff", date1: "2018-02-21T12:36:33-08:00"]]

    }

    def "parseMapTypeEntries"() {
        given:
        when:
        def result = ParamsUtil.parseMapTypeEntries(input)
        then:
        result == expect
        where:
        input                                                       || expect
        [:]                                                         || [:]
        ['a._type': 'map']                                          || ['a._type': 'map']
        ['a._type': 'map', 'a.map': 'x']                            || ['a._type': 'map', 'a.map': 'x']
        ['a._type': 'map', 'a.map': [:]]                            || [a: [:]]
        ['a._type': 'map', 'a.map': [:], 'a.extra': 'asdf']         || [a: [:]]
        ['a._type': 'zmap', 'a.map': [:]]                           || ['a._type': 'zmap', 'a.map': [:]]
        ['a._type': 'map', 'a.map': ['0.key': 'z', '0.value': 'x']] || [a: [z: 'x']]
        ['a._type': 'map', 'a.map': ['0.key': 'z', '0.value': 'x'],
         'b._type': 'map', 'b.map': ['0.key': 'p', '0.value': 'q']] || [a: [z: 'x'], b: [p: 'q']]
    }

    def "parseIndexedMap"() {
        given:

        when:
        def result = ParamsUtil.parseIndexedMapParams(input)
        then:
        result != null
        result == expect

        where:
        input                                                               || expect
        ['0.key': 'abc', '0.value': 'xyz']                                   | [abc: 'xyz']
        ['0.key': 'abc', '0.value': '']                                      | [abc: '']
        ['0.key': 'abc']                                                     | [abc: '']
        ['0.key': '', '0.value': 'xyz']                                      | [:]
        ['blah': 'blee']                                                     | [:]
        ['0.key': 'abc', '0.value': 'xyz', '1.key': 'def', '1.value': 'pqr'] | [abc: 'xyz', def: 'pqr']
        ['0.key': '', '0.value': 'xyz', '1.key': 'def', '1.value': 'pqr']    | [def: 'pqr']
        ['0.key': '', '1.key': 'def', '1.value': 'pqr']                      | [def: 'pqr']
        ['0.key': 'abc', '0.value': 'xyz', '1.key': 'def', '2.value': 'pqr'] | [abc: 'xyz', def: '']
        ['0.key': 'abc', '0.value': 'xyz', '2.key': 'def', '1.value': 'pqr'] | [abc: 'xyz']
        ['0.key': 'abc', '0.value': 'xyz', 'blah': 'blee']                   | [abc: 'xyz']
        ['0.key': 'abc', '0.value': 'xyz', '2.key': 'def', '2.value': 'pqr'] | [abc: 'xyz']

    }
}

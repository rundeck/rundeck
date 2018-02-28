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

package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ValidatorSpec extends Specification {
    @Unroll
    def "validateProperties options can have collection values"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().options('testOptionsDynamic').dynamicValues(true).build(),
            PropertyBuilder.builder().select('testSelectStatic').values(['asdf', 'xyz']).build(),
            PropertyBuilder.builder().options('testOptionsStatic').values(['3asdf', '3xyz']).build(),
            PropertyBuilder.builder().string('testString').build(),
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid
        report.errors == errMap

        where:
        input                                  || isValid | errMap
        [testOptionsDynamic: 'asdf']           || true    | [:]
        [testOptionsDynamic: ['abc', 'def']]   || true    | [:]

        [testSelectStatic: 'asdf']             || true    | [:]
        [testSelectStatic: 'xyz']              || true    | [:]
        [testSelectStatic: ['asdf']]           || false   | [testSelectStatic: 'Invalid data type: expected a String']
        [testSelectStatic: ['xyz']]            || false   | [testSelectStatic: 'Invalid data type: expected a String']
        [testSelectStatic: ['abc', 'def']]     || false   | [testSelectStatic: 'Invalid data type: expected a String']

        [testOptionsStatic: '3asdf']           || true    | [:]
        [testOptionsStatic: '3xyz']            || true    | [:]
        [testOptionsStatic: 'asdf']            || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']
        [testOptionsStatic: ['3asdf', '3xyz']] || true    | [:]
        [testOptionsStatic: ['3asdf']]         || true    | [:]
        [testOptionsStatic: ['3xyz']]          || true    | [:]
        [testOptionsStatic: ['3asdf', 'xyz']]  || false   | [testOptionsStatic: 'Invalid value(s): [xyz]']
        [testOptionsStatic: ['asdf', '3xyz']]  || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']
        [testOptionsStatic: ['asdf']]          || false   | [testOptionsStatic: 'Invalid value(s): [asdf]']

        [testString: 'ok']                     || true    | [:]
        [testString: ['notok']]                || false   | [testString: 'Invalid data type: expected a String']
        [testString: [a: 'b']]                 || false   | [testString: 'Invalid data type: expected a String']
    }

    @Unroll
    def "validateProperties map requires a map"() {
        given:
        def resolver = PropertyResolverFactory.createInstanceResolver(input)
        def defScope = PropertyScope.Instance
        def igScope = null
        def props = [
            PropertyBuilder.builder().name('testMap').type(Property.Type.Map).dynamicValues(true).build(),
        ]
        when:
        def report = Validator.validateProperties(resolver, props, defScope, igScope)
        then:
        report.valid == isValid

        report.errors['testMap'] == errMessage


        where:
        input                     || isValid | errMessage
        [testMap: 'asdf']         || false   | 'Invalid data type: expected a Map'
        [testMap: ['abc', 'def']] || false   | 'Invalid data type: expected a Map'
        [testMap: [asdf: 'xyz']]  || true    | null

    }
}

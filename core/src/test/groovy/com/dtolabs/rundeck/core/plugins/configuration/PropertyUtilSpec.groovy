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

import spock.lang.Specification

class PropertyUtilSpec extends Specification {
    def "options validator check"() {
        given:
        def validator = validSet ? { String a -> a in validSet } as PropertyValidator : null
        when:
        def prop = PropertyUtil.forType(
            Property.Type.Options,
            'asdf',
            'Asdf',
            'something',
            false,
            null,
            ['asdf'],
            [:],
            validator,
            null,
            PropertyScope.InstanceOnly,
            [:],
            dynamic
        )
        then:
        isValid == prop.validator.isValid(testValue)

        where:
        dynamic | testValue  | validSet        || isValid
        true    | 'asdf'     | null            || true
        false   | 'asdf'     | null            || true
        true    | 'not-asdf' | null            || true
        false   | 'abc'      | null            || false
        false   | 'abc'      | ['abc']         || false
        true    | 'abc'      | ['abc']         || true
        true    | 'asdf'     | ['abc']         || false
        true    | 'asdf'     | ['abc', 'asdf'] || true
    }
}

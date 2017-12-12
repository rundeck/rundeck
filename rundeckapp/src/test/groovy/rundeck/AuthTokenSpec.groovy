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

package rundeck

import spock.lang.Specification

/**
 * @author greg
 * @since 3/16/17
 */
class AuthTokenSpec extends Specification {
    def "parseAuthRoles"() {

        when:
        def result = AuthToken.parseAuthRoles(input)

        then:
        result == expected as Set

        where:
        input          | expected
        'a'            | ['a']
        'a,b'          | ['a', 'b']
        'a,b'          | ['b', 'a']
        'a  ,  b'      | ['a', 'b']
        '   a  ,  b  ' | ['a', 'b']
        null           | []
        ''             | []
        '  '           | []
    }

    def "generate"() {
        when:
        def result = AuthToken.generateAuthRoles(input)

        then:
        result == expected

        where:
        input                | expected
        ['a', 'b']           | 'a,b'
        ['asdf']             | 'asdf'
        ['', 'b']            | 'b'
        ['   ', 'b', '    '] | 'b'
    }
}

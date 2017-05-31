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

package com.dtolabs.rundeck.core.data

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/31/17
 */
class ScriptVarExpanderSpec extends Specification {


    @Unroll
    def "parse script var valid"() {
        given:
        when:
        def result = new ScriptVarExpander().parseVariable(str)
        then:
        result.step == step
        result.group == group
        result.key == key
        result.node == node

        where:
        str           | step | group | key | node
        'a.b'         | null | 'a'   | 'b' | null
        'a.b/badnode' | null | 'a'   | 'b' | 'badnode'
        '2:a.b'       | "2"  | 'a'   | 'b' | null
        '2:a.b/node1' | "2"  | 'a'   | 'b' | 'node1'
    }

    @Unroll
    def "parse script var invalid"() {
        given:
        when:
        def result = new ScriptVarExpander().parseVariable(str)
        then:
        result == null

        where:
        str            | _
        'a'            | _
        'a .b/node'    | _
        '2: a.b'       | _
        '2:a.b /node1' | _
    }
}

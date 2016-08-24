/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * Created by greg on 6/21/16.
 */
@TestFor(UtilityTagLib)
class UtilityTagLibSpec extends Specification {
    def "text after"() {
        given:
        def text = '''abc
123
456
---
789
'''
        when:
        def result = tagLib.textAfterLine(text: text, marker: '---').toString()

        then:
        result == '789\n'

    }
    def "text before"() {
        given:
        def text = '''abc
123
456
---
789
'''
        when:
        def result = tagLib.textBeforeLine(text: text, marker: '---').toString()

        then:
        result == '''abc
123
456'''

    }
}

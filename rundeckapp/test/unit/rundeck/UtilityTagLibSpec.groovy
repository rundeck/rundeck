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
import org.springframework.context.MessageSource
import rundeck.codecs.HTMLAttributeCodec
import spock.lang.Specification
import spock.lang.Unroll

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

    @Unroll
    def "relativeDateString optional html output"() {
        given:
        messageSource.addMessages(['format.time.sec.abbrev' : '{0}s',
                                   'format.time.min.abbrev' : '{0}m',
                                   'format.time.hour.abbrev': '{0}h',
                                   'format.time.day.abbrev' : '{0}d'], request.locale
        )

        Date now = new Date()
        Date then = new Date(now.time - diff)
        def codec1 = mockCodec(HTMLAttributeCodec)

        expect:
        expect == tagLib.relativeDateString(
                [
                        start     : reverse ? now : then, end: reverse ? then : now,
                        html      : isHtml,
                        agoClass  : agoClass,
                        untilClass: untilClass
                ]
        )



        where:
        isHtml | agoClass | untilClass | reverse | diff           || expect
        false  | null     | null       | false   | 12000          || '12s'
        false  | null     | null       | true    | 12000          || '12s'
        false  | null     | null       | false   | 120000         || '2m'
        false  | null     | null       | false   | 125000         || '2m5s'
        false  | null     | null       | false   | 3600000        || '1h'
        false  | null     | null       | false   | 5400000        || '1h30m'
        false  | null     | null       | false   | (24 * 3600000) || '1d'
        false  | null     | null       | false   | (26 * 3600000) || '1d2h'
        true   | null     | null       | false   | 12000          || '<span class="ago">12s</span>'
        true   | 'Xago2'  | null       | false   | 12000          || '<span class="Xago2">12s</span>'
        true   | null     | null       | true    | 12000          || '<span class="until">12s</span>'
        true   | null     | 'xUntil3'  | true    | 12000          || '<span class="xUntil3">12s</span>'
    }
}

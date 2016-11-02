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

package com.dtolabs.rundeck.core.dispatcher

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 11/2/16.
 */
class DataContextUtilsSpec extends Specification {
    @Unroll
    def "replace tokens in script duplicate start char #script"() {
        given:
        Framework fwk = null
        File dest = File.createTempFile('test', 'tmp')

        when:
        File result = DataContextUtils.replaceTokensInScript(
                script,
                context,
                fwk,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest
        )


        then:
        result.text == expect

        where:

        script                                | context                       | expect
        'abc'                                 | [a: [b: 'bcd']]               | 'abc\n'
        'echo \'hello@@option.domain@\''      | [:]                           | 'echo \'hello@\'\n'
        'echo \'hello@milk @option.domain@\'' | [:]                           | 'echo \'hello@milk \'\n'
        'echo \'hello@@option.domain@\''      | [option: [domain: 'peabody']] | 'echo \'hello@peabody\'\n'
        'echo \'hello@milk @option.domain@\'' | [option: [domain: 'peabody']] | 'echo \'hello@milk peabody\'\n'
        'echo \'hello@milk@option.domain@\''  | [option: [domain: 'peabody']] | 'echo \'hellooption.domain@\'\n'
        'bloo hello@@@nothing@'               | [:]                           | 'bloo hello@@\n'
        'bloo hello@@@ending'                 | [:]                           | 'bloo hello@@@ending\n'
        'bloo hello@ending'                   | [:]                           | 'bloo hello@ending\n'
        'bloo hello@@@ending\n'               | [:]                           | 'bloo hello@@@ending\n'
    }

    @Unroll
    def "replace tokens in script escaped start char #script"() {
        given:
        Framework fwk = null
        File dest = File.createTempFile('test', 'tmp')

        when:
        File result = DataContextUtils.replaceTokensInScript(
                script,
                context,
                fwk,
                ScriptfileUtils.LineEndingStyle.UNIX,
                dest
        )


        then:
        result.text == expect

        where:

        script                                  | context                       | expect
        'abc'                                   | [a: [b: 'bcd']]               | 'abc\n'
        'a\\bc'                                 | [a: [b: 'bcd']]               | 'a\\bc\n'
        'echo \'hello\\@@option.domain@\''      | [:]                           | 'echo \'hello@\'\n'
        'echo \'hello\\@milk @option.domain@\'' | [:]                           | 'echo \'hello@milk \'\n'
        'echo \'hello\\@@option.domain@\''      | [option: [domain: 'peabody']] | 'echo \'hello@peabody\'\n'
        'echo \'hello\\@milk@option.domain@\''  | [option: [domain: 'peabody']] | 'echo \'hello@milkpeabody\'\n'
    }
}

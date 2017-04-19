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

package com.dtolabs.rundeck.core.utils

import spock.lang.Specification

/**
 * @author greg
 * @since 4/19/17
 */
class ScriptExecUtilSpec extends Specification {
    def "expand data in scriptinterpreter"() {
        given:
        Map<String, Map<String, String>> dcontext = [option: ["blah": "sam"], globals: ["sudo": "/bin/sudo"]]
        String scriptargs = argstring
        String[] scriptargsarr = argsarray
        when:
        def result = ScriptExecUtil.createScriptArgs(
                dcontext,
                scriptargs,
                scriptargsarr,
                scriptinterpreter,
                quoted,
                '/a/file'
        )

        then:
        result == expected

        where:
        argstring | argsarray    | scriptinterpreter        | quoted | expected
        null      | ["a", "arg"] | 'bob'                    | false  | ['bob', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${option.blah}'         | false  | ['sam', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${globals.sudo}'        | false  | ['/bin/sudo', '/a/file', 'a', 'arg']
        null      | ["a", "arg"] | '${globals.sudo} myexec' | false  | ['/bin/sudo', 'myexec', '/a/file', 'a', 'arg']
    }
}

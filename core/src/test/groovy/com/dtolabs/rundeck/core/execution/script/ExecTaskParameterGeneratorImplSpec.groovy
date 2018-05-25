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

package com.dtolabs.rundeck.core.execution.script

import com.dtolabs.rundeck.core.common.INodeEntry
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 7/7/16.
 */
@Unroll
class ExecTaskParameterGeneratorImplSpec extends Specification {
    def "osFamily should not be case sensitive #osFamily"() {
        given:
        def sut = new ExecTaskParameterGeneratorImpl()
        def node = Mock(INodeEntry) {
            getOsFamily() >> osFamily
        }


        when:
        def result = sut.generate(node, true, null, ['a', 'command'] as String[])


        then:
        result.commandArgs == cmdArgs


        where:
        osFamily  | cmdExec     | cmdArgs
        'windows' | 'cmd.exe'   | ['/c', 'a', 'command']
        'Windows' | 'cmd.exe'   | ['/c', 'a', 'command']
        'unix'    | '/bin/bash' | ['-c', 'a command']
        'other'   | '/bin/bash' | ['-c', 'a command']


    }
}

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

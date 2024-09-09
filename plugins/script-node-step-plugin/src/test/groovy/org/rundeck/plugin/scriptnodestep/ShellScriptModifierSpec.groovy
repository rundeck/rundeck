package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import spock.lang.Specification

class ShellScriptModifierSpec extends Specification {
    def "process input"() {
        given:
            def modifier = new ShellScriptModifier()
            def list = []
            def sink = { input -> list << input } as FileCopierUtil.ContentModifier.Sink
        when:
            def result = modifier.process(line, sink)
        then:
            !result
            list == expect
        where:
            line          | expect
            '#!/bin/bash' | ['#!/bin/bash', ShellScriptModifier.EVAL_STRING]
            'some script' | [ShellScriptModifier.EVAL_STRING, 'some script']

    }
}

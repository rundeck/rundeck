package org.rundeck.plugin.scriptnodestep

import spock.lang.Specification

class ShellScriptModifierSpec extends Specification {
    def "append eval line"() {
        given:
            def modifier = new ShellScriptModifier()
        when:
            def result = modifier.modifyScriptForSecureInput(script)
        then:
            result == expect
        where:
            script                      | expect
            '#!/bin/bash\necho hello'   | '#!/bin/bash\n' + ShellScriptModifier.EVAL_STRING + 'echo hello'
            '#!/bin/zsh\n\nsome script' | '#!/bin/zsh\n' + ShellScriptModifier.EVAL_STRING + '\nsome script'
            'some script'               | ShellScriptModifier.EVAL_STRING + 'some script'

    }
}

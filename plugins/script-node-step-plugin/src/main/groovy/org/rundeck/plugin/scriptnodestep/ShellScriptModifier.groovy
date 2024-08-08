package org.rundeck.plugin.scriptnodestep

import groovy.transform.CompileStatic

/**
 * Modify a shell script to include the eval line to consume stdin data as rundeck variables.
 * Appends the eval line after the first line if it a shebang line, otherwise prepends it to the script.
 */
@CompileStatic
class ShellScriptModifier implements ScriptModifier {
    static String EVAL_STRING = 'eval $(</dev/stdin) #consume rundeck variables\n'

    String modifyScriptForSecureInput(String script) {
        StringBuilder sb = new StringBuilder()
        int index = 0
        if (script.startsWith('#!')) {
            //skip shebang
            index = script.indexOf('\n')
            if (index > 0) {
                sb.append(script.substring(0, index + 1))
                index++
            } else {
                index = 0
            }
        }
        sb.append(EVAL_STRING)
        sb.append(script.substring(index))

        return sb.toString()
    }
}

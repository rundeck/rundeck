package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import groovy.transform.CompileStatic
/**
 * Modify a shell script to include the eval line to consume stdin data as rundeck variables.
 * Appends the eval line after the first line if it a shebang line, otherwise prepends it to the script.
 */
@CompileStatic
class ShellScriptModifier implements FileCopierUtil.ContentModifier {
    static String EVAL_STRING = 'eval $(</dev/stdin) #consume rundeck variables'
    static final String SHEBANG = '#!'

    @Override
    boolean process(final String s, final Sink sink) throws IOException {
        if (s.startsWith(SHEBANG)) {
            sink.writeLine(s)
            sink.writeLine(EVAL_STRING)
        } else {
            sink.writeLine(EVAL_STRING)
            sink.writeLine(s)
        }
        return false
    }
}

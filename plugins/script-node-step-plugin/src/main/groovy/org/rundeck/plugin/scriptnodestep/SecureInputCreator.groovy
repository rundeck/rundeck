package org.rundeck.plugin.scriptnodestep


import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContext
import groovy.transform.CompileStatic

/**
 * Creates necessary input data to pass variable declarations of secure values to a script
 */
@CompileStatic
class SecureInputCreator implements ScriptInputCreator {

    final ShellUtil shellUtil

    SecureInputCreator(final ShellUtil shellUtil) {
        this.shellUtil = shellUtil
    }

    @Override
    InputStream createInputForProcess(final ExecutionContext context) {
        Map<String, String> env = createEnvForProcess(context);
        if (env == null) {
            return null;
        }
        return new ByteArrayInputStream(createSecureEnvString(env).getBytes());
    }


    private static Map<String, String> createEnvForProcess(final ExecutionContext context) {
        final Map<String, Map<String, String>> dataContext = context.getPrivateDataContext();
        if (null == dataContext || dataContext.isEmpty()) {
            return null;
        }
        final Map<String, String> secureOption = dataContext.get("option");
        if (null == secureOption || secureOption.isEmpty()) {
            return null;
        }
        Map<String, String> env =
            DataContextUtils.generateEnvVarsFromContext(DataContextUtils.context("option", secureOption));
        return env;
    }

    private String createSecureEnvString(final Map<String, String> env) {
        final StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            shellUtil.appendEnvLine(entry.key, entry.value, sb)
        }
        return sb.toString()
    }
}

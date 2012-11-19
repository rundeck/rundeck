/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* BaseScriptPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 2:12 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * BaseScriptPlugin is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseScriptPlugin extends AbstractDescribableScriptPlugin {
    protected BaseScriptPlugin(ScriptPluginProvider provider, Framework framework) {
        super(provider, framework);
    }

    /**
     * Run a command with environment variables in a working dir, and copy the streams
     */
    protected int runScript(final String[] command,
                            final Map<String, String> envMap, final File workingdir,
                            final OutputStream outputStream, final OutputStream errorStream)
        throws IOException, InterruptedException {
        final String[] envarr = createEnvironmentArray(envMap);

        int result = -1;
        Thread errthread = null;
        Thread outthread = null;
        final Runtime runtime = Runtime.getRuntime();
        final Process exec = runtime.exec(command, envarr, workingdir);
        errthread = Streams.copyStreamThread(exec.getErrorStream(), errorStream);
        outthread = Streams.copyStreamThread(exec.getInputStream(), outputStream);
        errthread.start();
        outthread.start();
        exec.getOutputStream().close();
        result = exec.waitFor();
        errthread.join();
        outthread.join();
        return result;
    }

    /**
     * Create the environment array for executing via {@link Runtime}.
     */
    private String[] createEnvironmentArray(final Map<String, String> envMap) {
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final Map.Entry<String, String> entry : envMap.entrySet()) {
            envlist.add(entry.getKey() + "=" + entry.getValue());
        }
        return envlist.toArray(new String[envlist.size()]);
    }

    /**
     * Runs the script configured for the script plugin and channels the output to two streams.
     * the
     */
    protected int runPluginScript(final ExecutionContext executionContext,
                                  final PluginStepItem item,
                                  final PrintStream outputStream, final PrintStream errorStream)
        throws IOException, InterruptedException {
        final Map<String, Map<String, String>> localDataContext = createScriptDataContext(executionContext, item);
        final String[] finalargs = createScriptArgs(localDataContext);

        executionContext.getExecutionListener().log(3, "[" + getProvider().getName() + "] executing: " + Arrays.asList(
            finalargs));

        return runScript(finalargs,
                           DataContextUtils.generateEnvVarsFromContext(localDataContext),
                           null,
                           outputStream,
                           errorStream
        );
    }

    /**
     * Create a data context containing the plugin values "file","scriptfile" and "base", as well as all config
     * values.
     */
    protected Map<String, Map<String, String>> createScriptDataContext(final ExecutionContext executionContext,
                                                                       final PluginStepItem item) {
        final Map<String, Map<String, String>> localDataContext
            = ScriptDataContextUtil.createScriptDataContextForProject(
            executionContext.getFramework(),
            executionContext.getFrameworkProject());
        localDataContext.get("plugin").putAll(createPluginDataContext());
        localDataContext.putAll(executionContext.getDataContext());

        final HashMap<String, String> configMap = new HashMap<String, String>();
        //convert values to string
        for (final Map.Entry<String, Object> entry : item.getStepConfiguration().entrySet()) {
            configMap.put(entry.getKey(), entry.getValue().toString());
        }
        localDataContext.put("config", configMap);
        return localDataContext;
    }

    /**
     * Create the command array for the data context.
     */
    protected String[] createScriptArgs(final Map<String, Map<String, String>> localDataContext) {

        final ScriptPluginProvider plugin = getProvider();
        final File scriptfile = plugin.getScriptFile();
        final String pluginname = plugin.getName();
        final String scriptargs = plugin.getScriptArgs();
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted = plugin.getInterpreterArgsQuoted();


        final ArrayList<String> arglist = new ArrayList<String>();
        if (null != scriptinterpreter) {
            arglist.addAll(Arrays.asList(scriptinterpreter.split(" ")));
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            final StringBuilder sbuf = new StringBuilder(scriptfile.getAbsolutePath());
            if (null != scriptargs) {
                sbuf.append(" ");
                sbuf.append(DataContextUtils.replaceDataReferences(scriptargs, localDataContext));
            }
            arglist.add(sbuf.toString());
        } else {
            arglist.add(scriptfile.getAbsolutePath());
            if (null != scriptargs) {
                arglist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(scriptargs.split(" "),
                                                                                    localDataContext)));
            }
        }
        return arglist.toArray(new String[arglist.size()]);
    }
}

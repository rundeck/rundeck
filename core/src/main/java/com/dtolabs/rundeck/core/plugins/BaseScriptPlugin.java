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
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * BaseScriptPlugin provides common methods for running scripts, used by the script plugin implementations.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseScriptPlugin extends AbstractDescribableScriptPlugin {
    protected BaseScriptPlugin(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }

    /**
     * Runs the script configured for the script plugin and channels the output to two streams.
     * @param executionContext context
     * @param outputStream output stream
     * @param errorStream error stream
     * @param framework fwlk
     * @param configuration configuration
     * @return exit code
     * @throws IOException          if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    protected int runPluginScript(final PluginStepContext executionContext,
                                  final PrintStream outputStream,
                                  final PrintStream errorStream,
                                  final Framework framework, final Map<String, Object> configuration)
        throws IOException, InterruptedException {
        final Map<String, Map<String, String>> localDataContext = createStepItemDataContext(
            framework,
            executionContext.getFrameworkProject(),
            executionContext.getDataContext(),
            configuration);
        final String[] finalargs = createScriptArgs(localDataContext);

        executionContext.getLogger().log(3, "[" + getProvider().getName() + "] executing: " + Arrays.asList(
            finalargs));

        return ScriptExecUtil.runLocalCommand(finalargs,
                                              DataContextUtils.generateEnvVarsFromContext(localDataContext),
                                              null,
                                              outputStream,
                                              errorStream
        );
    }

    /**
     * Create a data context containing the plugin values "file","scriptfile" and "base", as well as all config values.
     * @param framework fwk
     * @param project project name
     * @param context data context
     * @param configuration configuration
     * @return data context
     */
    protected Map<String, Map<String, String>> createStepItemDataContext(final Framework framework,
                                                                         final String project,
                                                                         final Map<String, Map<String, String>> context,
                                                                         final Map<String, Object> configuration) {

        final Map<String, Map<String, String>> localDataContext = createScriptDataContext(framework, project, context);

        final HashMap<String, String> configMap = new HashMap<String, String>();
        //convert values to string
        for (final Map.Entry<String, Object> entry : configuration.entrySet()) {
            configMap.put(entry.getKey(), entry.getValue().toString());
        }
        localDataContext.put("config", configMap);
        return localDataContext;
    }

    /**
     * create script data context
     * @param framework fwk
     * @param project project name
     * @param context orig context
     * @return new data context
     */
    protected Map<String, Map<String, String>> createScriptDataContext(final Framework framework,
                                                                       final String project,
                                                                       final Map<String, Map<String, String>> context) {
        final Map<String, Map<String, String>> localDataContext
            = ScriptDataContextUtil.createScriptDataContextForProject(framework, project);
        localDataContext.get("plugin").putAll(createPluginDataContext());
        localDataContext.putAll(context);
        return localDataContext;
    }

    /**
     * Create the command array for the data context.
     * @param localDataContext data
     * @return command array
     */
    protected String[] createScriptArgs(final Map<String, Map<String, String>> localDataContext) {

        final ScriptPluginProvider plugin = getProvider();
        final File scriptfile = plugin.getScriptFile();
        final String scriptargs = plugin.getScriptArgs();
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted = plugin.getInterpreterArgsQuoted();


        return ScriptExecUtil.createScriptArgs(localDataContext,
                                               scriptargs, null, scriptinterpreter, interpreterargsquoted,
                                               scriptfile.getAbsolutePath());
    }
    /**
     * Create the command array for the data context.
     * @param dataContext  data
     * @return arglist
     */
    protected ExecArgList createScriptArgsList(final Map<String, Map<String,
            String>> dataContext) {

        final ScriptPluginProvider plugin = getProvider();
        final File scriptfile = plugin.getScriptFile();
        final String scriptargs = null!=plugin.getScriptArgs()?
                DataContextUtils.replaceDataReferences(plugin.getScriptArgs(), dataContext) :
                    null;
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted = plugin.getInterpreterArgsQuoted();


        return ScriptExecUtil.createScriptArgList(scriptfile.getAbsolutePath(),
                scriptargs, null, scriptinterpreter, interpreterargsquoted);
    }

}

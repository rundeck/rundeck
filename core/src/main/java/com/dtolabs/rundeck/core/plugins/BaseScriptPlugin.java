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

/*
* BaseScriptPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 2:12 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.proxy.DefaultSecretBundle;
import com.dtolabs.rundeck.core.execution.proxy.ProxySecretBundleCreator;
import com.dtolabs.rundeck.core.execution.proxy.SecretBundle;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.utils.MapData;
import com.dtolabs.rundeck.core.utils.ScriptExecHelper;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import org.rundeck.storage.api.Resource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;


/**
 * BaseScriptPlugin provides common methods for running scripts, used by the script plugin implementations.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseScriptPlugin extends AbstractDescribableScriptPlugin implements ProxySecretBundleCreator {
    /**
     * can be replaced with test mock
     */
    private ScriptExecHelper scriptExecHelper = ScriptExecUtil.helper();

    protected BaseScriptPlugin(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }
    protected BaseScriptPlugin(final ScriptPluginProvider provider) {
        super(provider);
    }

    /**
     * Runs the script configured for the script plugin and channels the output to two streams.
     *
     * @param executionContext context
     * @param outputStream     output stream
     * @param errorStream      error stream
     * @param framework        fwlk
     * @param configuration    configuration
     * @return exit code
     *
     * @throws IOException          if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    protected int runPluginScript(
            final PluginStepContext executionContext,
            final PrintStream outputStream,
            final PrintStream errorStream,
            final Framework framework,
            final Map<String, Object> configuration
    )
            throws IOException, InterruptedException, ConfigurationException
    {
        Description pluginDesc = getDescription();
        final DataContext localDataContext = createScriptDataContext(
                framework,
                executionContext.getFrameworkProject(),
                executionContext.getDataContext()
        );
        Map<String, Object> instanceData = new HashMap<>(configuration);

        Map<String, String> data = MapData.toStringStringMap(instanceData);
        loadContentConversionPropertyValues(
                data,
                executionContext.getExecutionContext(),
                pluginDesc.getProperties()
        );
        localDataContext.merge(new BaseDataContext("config", data));

        final String[] finalargs = createScriptArgs(localDataContext);

        executionContext.getLogger().log(3, "[" + getProvider().getName() + "] executing: " + Arrays.asList(
                finalargs));

        Map<String, String> envMap = new HashMap<>();
        if (isMergeEnvVars()) {
            envMap.putAll(getScriptExecHelper().loadLocalEnvironment());
        }
        envMap.putAll(DataContextUtils.generateEnvVarsFromContext(localDataContext));
        return getScriptExecHelper().runLocalCommand(
                finalargs,
                envMap,
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
    protected DataContext createScriptDataContext(
            final Framework framework,
            final String project,
            final Map<String, Map<String, String>> context) {
        BaseDataContext localDataContext = new BaseDataContext();
        localDataContext.merge(ScriptDataContextUtil.createScriptDataContextObjectForProject(framework, project));
        localDataContext.group("plugin").putAll(createPluginData());
        localDataContext.putAll(context);
        return localDataContext;
    }
    protected DataContext createScriptDataContext(
            final Map<String, Map<String, String>> context) {
        BaseDataContext localDataContext = new BaseDataContext();
        localDataContext.group("plugin").putAll(createPluginData());
        if (null != context) {
            localDataContext.putAll(context);
        }
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
        final String[] scriptargsarray = plugin.getScriptArgsArray();
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted = plugin.getInterpreterArgsQuoted();


        return getScriptExecHelper().createScriptArgs(
                localDataContext,
                scriptargs, scriptargsarray, scriptinterpreter, interpreterargsquoted,
                scriptfile.getAbsolutePath()
        );
    }
    /**
     * Create the command array for the data context.
     * @param dataContext  data
     * @return arglist
     */
    protected ExecArgList createScriptArgsList(final Map<String, Map<String, String>> dataContext) {

        final ScriptPluginProvider plugin = getProvider();
        final File scriptfile = plugin.getScriptFile();
        final String scriptargs = null != plugin.getScriptArgs() ?
                                  DataContextUtils.replaceDataReferencesInString(plugin.getScriptArgs(), dataContext) :
                                  null;
        final String[] scriptargsarr = null!=plugin.getScriptArgsArray() ?
                                       DataContextUtils.replaceDataReferencesInArray(plugin.getScriptArgsArray(), dataContext) :
                                       null;
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted = plugin.getInterpreterArgsQuoted();


        return getScriptExecHelper().createScriptArgList(scriptfile.getAbsolutePath(),
                scriptargs, scriptargsarr, scriptinterpreter, interpreterargsquoted);
    }

    public ScriptExecHelper getScriptExecHelper() {
        return scriptExecHelper;
    }

    public void setScriptExecHelper(ScriptExecHelper scriptExecHelper) {
        this.scriptExecHelper = scriptExecHelper;
    }


    @Override
    public SecretBundle prepareSecretBundle(
            final ExecutionContext context, final INodeEntry node
    ) {
        Description pluginDesc = getDescription();

        final PropertyResolver resolver = PropertyResolverFactory.createPluginRuntimeResolver(
                context,
                loadInstanceDataFromNodeAttributes(node, pluginDesc),
                getProvider().getService(),
                getProvider().getName()
        );

        final Map<String, Object> config =
                PluginAdapterUtility.mapDescribedProperties(
                        resolver,
                        pluginDesc,
                        PropertyScope.Instance
                );

        return generateBundle(context, config);
    }

    @Override
    public SecretBundle prepareSecretBundleWorkflowStep(ExecutionContext context, Map<String, Object> configuration) {
        return generateBundle(context, configuration);
    }

    @Override
    public SecretBundle prepareSecretBundleWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration) {
        return generateBundle(context, configuration);
    }

    @Override
    public List<String> listSecretsPath(ExecutionContext context, INodeEntry node) {
        Description pluginDesc = getDescription();

        final PropertyResolver resolver = PropertyResolverFactory.createPluginRuntimeResolver(
                context,
                loadInstanceDataFromNodeAttributes(node, pluginDesc),
                getProvider().getService(),
                getProvider().getName()
        );

        final Map<String, Object> config =
                PluginAdapterUtility.mapDescribedProperties(
                        resolver,
                        pluginDesc,
                        PropertyScope.Instance
                );
        return generateListStoragePath(context, config);
    }

    @Override
    public List<String> listSecretsPathWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration) {
        return generateListStoragePath(context, configuration);
    }

    @Override
    public List<String> listSecretsPathWorkflowStep(ExecutionContext context, Map<String, Object> configuration) {
        return generateListStoragePath(context, configuration);
    }

    private SecretBundle generateBundle(ExecutionContext context, Map<String, Object> configuration){

        List<String> listStoragePaths = generateListStoragePath(context, configuration);
        DefaultSecretBundle bundle = new DefaultSecretBundle();

        for (String propValue : listStoragePaths) {
            Resource<ResourceMeta> r = context.getStorageTree().getResource(propValue);
            if(r != null) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    r.getContents().writeContent(byteArrayOutputStream);
                    bundle.addSecret(propValue, byteArrayOutputStream.toByteArray());
                } catch (IOException iex) {
                    context.getExecutionLogger().log(0,String.format("IOException Unable to add secret value to secret bundle for: %s",propValue));
                }
            }
        }
        return bundle;
    }


    private List<String> generateListStoragePath(ExecutionContext context, Map<String, Object> configuration){
        List<String> listStoragePath = new ArrayList<>();

        final Map<String, Map<String, String>> localDataContext = createScriptDataContext(
                context.getFramework(),
                context.getFrameworkProject(),
                context.getDataContext()
        );

        Map<String, Object> expanded =
                DataContextUtils.replaceDataReferences(
                        configuration,
                        localDataContext
                );


        Description pluginDesc = getDescription();
        Map<String, String> data = MapData.toStringStringMap(expanded);
        for (Property property : pluginDesc.getProperties()) {
            String name = property.getName();
            String propValue = data.get(name);
            if (null == propValue) {
                continue;
            }
            Map<String, Object> renderingOptions = property.getRenderingOptions();
            if (renderingOptions != null) {
                Object conversion = renderingOptions.get(StringRenderingConstants.VALUE_CONVERSION_KEY);
                if (StringRenderingConstants.ValueConversion.STORAGE_PATH_AUTOMATIC_READ.equalsOrString(conversion)) {
                    listStoragePath.add(propValue);
                }
            }
        }
        return listStoragePath;
    }
}

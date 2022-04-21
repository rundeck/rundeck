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
* StepPluginAdapter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 6:30 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.SerializableExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResultImpl;
import com.dtolabs.rundeck.core.http.ApacheHttpClient;
import com.dtolabs.rundeck.core.http.HttpClient;
import com.dtolabs.rundeck.core.http.RequestProcessor;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.rundeck.app.spi.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * StepPluginAdapter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class StepPluginAdapter implements StepExecutor, Describable, DynamicProperties{
    protected static Logger log = LoggerFactory.getLogger(StepPluginAdapter.class);
    public static final Convert CONVERTER = new Convert();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String remotePluginService = System.getenv("RD_REMOTE_PLUGIN_SERVICE");
    List<String> REMOTABLE_PLUGINS = Arrays.asList("edu.ohio.ais.rundeck.HttpWorkflowStepPlugin",
            "com.batix.rundeck.plugins.AnsiblePlaybookWorkflowStep",
            "com.batix.rundeck.plugins.AnsiblePlaybookInlineWorkflowStep",
            "com.batix.rundeck.plugins.AnsibleModuleWorkflowStep");
    private static final Pattern LOG_LEVEL_MARKER = Pattern.compile("^==([0-5])==$");
    private StepPlugin plugin;

    public StepPluginAdapter(final StepPlugin plugin) {
        this.plugin = plugin;
    }

    static class Convert implements Converter<StepPlugin, StepExecutor> {
        @Override
        public StepExecutor convert(final StepPlugin plugin) {
            return new StepPluginAdapter(plugin);
        }
    }

    @Override
    public Map<String, Object> dynamicProperties(Map<String, Object> projectAndFrameworkValues, Services services){
        if(plugin instanceof DynamicProperties){
            return ((DynamicProperties)plugin).dynamicProperties(projectAndFrameworkValues, services);
        }

        return null;
    }

    @Override
    public Description getDescription() {
        if (plugin instanceof Describable) {
            final Describable desc = (Describable) plugin;
            return desc.getDescription();
        } else {
            return PluginAdapterUtility.buildDescription(plugin, DescriptionBuilder.builder());
        }
    }

    @Override
    public boolean isNodeDispatchStep(StepExecutionItem item) {
        return false;
    }

    @Override
    public StepExecutionResult executeWorkflowStep(final StepExecutionContext executionContext,
                                                   final StepExecutionItem item) throws StepException
        {
        Map<String, Object> instanceConfiguration = getStepConfiguration(item);
            Description description = getDescription();
            Map<String,Boolean> blankIfUnexMap = new HashMap<>();
            if(description != null) {
                description.getProperties().forEach(p -> {
                    blankIfUnexMap.put(p.getName(), p.isBlankIfUnexpandable());
                });
            }
        if (null != instanceConfiguration) {
            instanceConfiguration = SharedDataContextUtils.replaceDataReferences(
                    instanceConfiguration,
                    ContextView.global(),
                    ContextView::nodeStep,
                    null,
                    executionContext.getSharedDataContext(),
                    false,
                    blankIfUnexMap
            );
        }
        final String providerName = item.getType();
        final PropertyResolver resolver = PropertyResolverFactory.createStepPluginRuntimeResolver(executionContext,
                instanceConfiguration,
                ServiceNameConstants.WorkflowStep,
                providerName
        );
        final PluginStepContext stepContext = PluginStepContextImpl.from(executionContext);
        final Map<String, Object> config = PluginAdapterUtility.configureProperties(resolver, description,
                plugin, PropertyScope.InstanceOnly);

        if(isRemoteable()) {
            String oid = null;
            ApacheHttpClient client = new ApacheHttpClient();
            try {
                oid = serializeContext(client, remotePluginService, item, instanceConfiguration, stepContext);
                log.info("Saved workflow context with key {}", oid);
            } catch (Exception ex) {
                return new StepExecutionResultImpl(ex,StepFailureReason.RemoteFailed,"Unable to serialize context for remote plugin execution");
            }

            CompletableFuture<StepExecutionResultImpl> ftr = new CompletableFuture<>();
            try {
                client.setUri(URI.create(String.format("%s/run/%s", remotePluginService, oid)));
                client.setMethod(HttpClient.Method.POST);
                client.addPayload("application/json", new ObjectMapper().writeValueAsString(instanceConfiguration));
                client.execute(new RequestProcessor<HttpResponse>() {
                    @Override
                    public void accept(HttpResponse httpResponse) throws Exception {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
                        Matcher m;
                        int logLevel = 3;
                        String line = null;

                        while((line = reader.readLine()) != null) {
                            m = LOG_LEVEL_MARKER.matcher(line);
                            if(m.matches()) {
                                logLevel = Integer.parseInt(m.group(1));
                            } else {
                                executionContext.getExecutionListener().log(logLevel, line);
                            }
                        }
                        if(httpResponse.getStatusLine().getStatusCode() == 200) {
                            ftr.complete(new StepExecutionResultImpl());
                        } else {
                            ftr.complete(new StepExecutionResultImpl(new Exception("Remote plugin service returned error"), StepFailureReason.RemoteFailed, String.format("Remote plugin service returned http code: %d", httpResponse.getStatusLine().getStatusCode())));
                        }
                    }
                });
            } catch (Exception ex) {
                ftr.complete(new StepExecutionResultImpl(ex,StepFailureReason.PluginFailed,"Exception when executing remote plugin service call"));
            }
            try {
                return ftr.get();
            } catch (Exception ex) {
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL,
                        "Failed executing step plugin [" + providerName + "]: remote service failed");
                return new StepExecutionResultImpl(ex, StepFailureReason.RemoteFailed, ex.getMessage());
            }

        }

        try {
            plugin.executeStep(stepContext, config);
        } catch (StepException e) {
            executionContext.getExecutionListener().log(
                    Constants.ERR_LEVEL,
                    e.getMessage()
            );
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL,
                    "Failed executing step plugin [" + providerName + "]: "
                    + stringWriter.toString()
            );

            return new StepExecutionResultImpl(e, e.getFailureReason(), e.getMessage());
        } catch (Throwable e) {
            final StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                    "Failed executing step plugin [" + providerName + "]: "
                            + stringWriter.toString());
            return new StepExecutionResultImpl(e, StepFailureReason.PluginFailed, e.getMessage());
        }
        return new StepExecutionResultImpl();
    }

    private boolean isRemoteable() {
        return remotePluginService != null &&
                REMOTABLE_PLUGINS.contains(getDescription().getName());
    }

    private String serializeContext(ApacheHttpClient client, String remotePluginService, StepExecutionItem item, Map<String, Object> instanceConfiguration, PluginStepContext context) throws Exception {
        try {
            SerializableExecutionContext scontext = new SerializableExecutionContext();
            scontext.setProjectProperties(context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()).getProperties());
            scontext.setPlugin(item.getType());
            scontext.setProject(context.getFrameworkProject());
            scontext.setLogLevel(context.getExecutionContext().getLoglevel());
            scontext.setPluginService(ServiceNameConstants.WorkflowStep);
            scontext.setInstanceConfiguration(instanceConfiguration);
            scontext.setDataContext(context.getExecutionContext().getDataContext());
            scontext.setPrivateContext(context.getExecutionContext().getPrivateDataContext());

            client.setUri(URI.create(String.format("%s/set/%s", remotePluginService, scontext.getOid())));
            client.setMethod(HttpClient.Method.POST);
            client.addPayload("application/json", mapper.writeValueAsString(scontext));
            client.execute(new RequestProcessor<HttpResponse>() {
                @Override
                public void accept(HttpResponse httpResponse) throws Exception {}
            });
            return scontext.getOid();
        } catch(Exception ex) {
            log.error("failed to serialize context",ex);
            throw ex;
        }
    }

    private Map<String, Object> getStepConfiguration(StepExecutionItem item) {
        if (item instanceof ConfiguredStepExecutionItem) {
            return ((ConfiguredStepExecutionItem) item).getStepConfiguration();
        } else {
            return null;
        }
    }

}

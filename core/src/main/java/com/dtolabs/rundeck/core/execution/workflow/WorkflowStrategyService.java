package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by greg on 5/5/16.
 */
public class WorkflowStrategyService extends ChainedProviderService<WorkflowStrategy> implements DescribableService,
        PluggableProviderService<WorkflowStrategy>
{
    private static final String SERVICE_NAME = ServiceNameConstants.WorkflowStrategy;
    private final Framework framework;
    private List<ProviderService<WorkflowStrategy>> serviceList;
    private final PluggableProviderService<WorkflowStrategy> pluginService;
    private final Map<String, String> builtinProviderSynonyms = new HashMap<>();
    private final BaseProviderRegistryService<WorkflowStrategy> builtinService;

    public String getName() {
        return SERVICE_NAME;
    }

    private WorkflowStrategyService(final Framework framework) {
        this.framework=framework;
        this.serviceList = new ArrayList<>();
        /*
         * WorkflowExecutionService chains several other services:
         * 1. builtin providers
         * 2. plugin providers
         */
        HashMap<String, Class<? extends WorkflowStrategy>> builtinProviders =
                new HashMap<String, Class<? extends WorkflowStrategy>>() {{
                    put(NodeFirstWorkflowStrategy.PROVIDER_NAME, NodeFirstWorkflowStrategy.class);
                    put(SequentialWorkflowStrategy.PROVIDER_NAME, SequentialWorkflowStrategy.class);
                    //backwards compatibility synonym
//                    put("step-first", SequentialWorkflowStrategy.class);
                    put(ParallelWorkflowStrategy.PROVIDER_NAME, ParallelWorkflowStrategy.class);
                }};
        builtinProviderSynonyms.put("step-first", SequentialWorkflowStrategy.PROVIDER_NAME);

        builtinService = ServiceFactory.builtinService(
                framework,
                SERVICE_NAME,
                builtinProviders
        );

        pluginService = ServiceFactory.pluginService(SERVICE_NAME, framework, WorkflowStrategy.class);


        serviceList.add(builtinService);
        serviceList.add(pluginService);
    }

    @Override
    protected List<ProviderService<WorkflowStrategy>> getServiceList() {
        return serviceList;
    }

    public static WorkflowStrategyService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final WorkflowStrategyService service = new WorkflowStrategyService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (WorkflowStrategyService) framework.getService(SERVICE_NAME);
    }

    /**
     * Get a configured strategy instance
     *
     * @param workflow workflow
     * @param config   config data
     *
     * @return instance with configuration applied
     *
     * @throws ExecutionServiceException
     */
    public WorkflowStrategy getStrategyForWorkflow(
            final WorkflowExecutionItem workflow,
            Map<String, Object> config,
            String projectName
    )
            throws ExecutionServiceException
    {

        String provider = workflow.getWorkflow().getStrategy();
        String s = builtinProviderSynonyms.get(provider);
        if (null != s) {
            provider = s;
        }
        WorkflowStrategy workflowStrategy = providerOfType(provider);
        if (null != config) {
            IRundeckProjectConfig iRundeckProjectConfig = framework.getFrameworkProjectMgr().loadProjectConfig(
                    projectName);

            PropertyResolver resolver = PropertyResolverFactory.createResolver(
                    config.size() > 0 ? PropertyResolverFactory.instanceRetriever(config) : null,
                    PropertyResolverFactory.instanceRetriever(
                            iRundeckProjectConfig.getProjectProperties()
                    ),
                    framework.getPropertyRetriever()
            );

            Description description = DescribableServiceUtil.descriptionForProvider(
                    true,
                    workflowStrategy
            );
            if (description != null) {
                config = PluginAdapterUtility.configureProperties(
                        resolver,
                        description,
                        workflowStrategy,
                        PropertyScope.Instance
                );
            }
        }
        return workflowStrategy;
    }
    /**
     * Get a configured strategy instance
     *
     * @param workflow workflow
     * @param resolver   config resolver
     *
     * @return instance with configuration applied
     *
     * @throws ExecutionServiceException
     */
    public WorkflowStrategy getStrategyForWorkflow(
            final WorkflowExecutionItem workflow,
            PropertyResolver resolver
    )
            throws ExecutionServiceException
    {

        String provider = workflow.getWorkflow().getStrategy();
        String s = builtinProviderSynonyms.get(provider);
        if (null != s) {
            provider = s;
        }
        WorkflowStrategy workflowStrategy = providerOfType(provider);
        if (null != resolver) {
            Description description = DescribableServiceUtil.descriptionForProvider(
                    true,
                    workflowStrategy
            );
            if (description != null) {
                Map<String, Object> stringObjectMap = PluginAdapterUtility.configureProperties(
                        resolver,
                        description,
                        workflowStrategy,
                        PropertyScope.Instance
                );
            }
        }
        return workflowStrategy;
    }

    public List<Description> listDescriptions() {
        return DescribableServiceUtil.listDescriptions(this);
    }

    public List<ProviderIdent> listDescribableProviders() {
        return DescribableServiceUtil.listDescribableProviders(this);
    }

    @Override
    public boolean isValidProviderClass(final Class clazz) {
        return pluginService.isValidProviderClass(clazz);
    }

    @Override
    public <X extends WorkflowStrategy> WorkflowStrategy createProviderInstance(final Class<X> clazz, final String name)
            throws PluginException, ProviderCreationException
    {
        return pluginService.createProviderInstance(clazz, name);
    }

    @Override
    public boolean isScriptPluggable() {
        return pluginService.isScriptPluggable();
    }

    @Override
    public WorkflowStrategy createScriptProviderInstance(final ScriptPluginProvider provider) throws PluginException {
        return pluginService.createScriptProviderInstance(provider);
    }

    public void registerClass(String name, Class<? extends WorkflowStrategy> clazz) {
        builtinService.registerClass(name, clazz);
    }

    public void registerInstance(String name, WorkflowStrategy object) {
        builtinService.registerInstance(name, object);
    }
}

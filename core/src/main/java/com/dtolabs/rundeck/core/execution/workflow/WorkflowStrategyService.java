package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig;
import com.dtolabs.rundeck.core.common.IServicesRegistration;
import com.dtolabs.rundeck.core.common.ProviderService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builtin and plugin providers for {@link WorkflowStrategy}
 * @author greg
 * @since 5/5/16
 */
public class WorkflowStrategyService extends ChainedProviderService<WorkflowStrategy> implements DescribableService,
        PluggableProviderService<WorkflowStrategy>
{
    private static final String SERVICE_NAME = ServiceNameConstants.WorkflowStrategy;
    private final IFramework framework;
    private final List<ProviderService<WorkflowStrategy>> serviceList;
    private final PluggableProviderService<WorkflowStrategy> pluginService;
    private final Map<String, String> builtinProviderSynonyms = new HashMap<>();
    private final ProviderRegistryService<WorkflowStrategy> builtinService;

    public String getName() {
        return SERVICE_NAME;
    }

    private WorkflowStrategyService(final IFramework framework) {
        this.framework=framework;
        this.serviceList = new ArrayList<>();
        /*
         * WorkflowExecutionService chains several other services:
         * 1. builtin providers
         * 2. plugin providers
         */
        HashMap<String, Class<? extends WorkflowStrategy>> builtinProviders = new HashMap<>();

        builtinProviders.put(NodeFirstWorkflowStrategy.PROVIDER_NAME, NodeFirstWorkflowStrategy.class);
        builtinProviders.put(SequentialWorkflowStrategy.PROVIDER_NAME, SequentialWorkflowStrategy.class);
        builtinProviders.put(ParallelWorkflowStrategy.PROVIDER_NAME, ParallelWorkflowStrategy.class);

        builtinProviderSynonyms.put("step-first", SequentialWorkflowStrategy.PROVIDER_NAME);

        builtinService = ServiceFactory.builtinService(SERVICE_NAME, builtinProviders);

        pluginService = ServiceFactory.pluginService(SERVICE_NAME, WorkflowStrategy.class, framework.getPluginManager());


        serviceList.add(builtinService);
        serviceList.add(pluginService);
    }

    @Override
    public boolean canLoadWithLoader(final ProviderLoader loader) {
        return pluginService.canLoadWithLoader(loader);
    }

    @Override
    public WorkflowStrategy loadWithLoader(final String providerName, final ProviderLoader loader)
        throws ProviderLoaderException
    {
        return pluginService.loadWithLoader(providerName,loader);
    }

    @Override
    public CloseableProvider<WorkflowStrategy> loadCloseableWithLoader(
        final String providerName, final ProviderLoader loader
    ) throws ProviderLoaderException
    {
        return pluginService.loadCloseableWithLoader(providerName,loader);
    }

    @Override
    protected List<ProviderService<WorkflowStrategy>> getServiceList() {
        return serviceList;
    }

    public static WorkflowStrategyService getInstanceForFramework(IFramework framework,
                                                                  final IServicesRegistration registration) {
        if (null == registration.getService(SERVICE_NAME)) {
            final WorkflowStrategyService service = new WorkflowStrategyService(framework);
            registration.setService(SERVICE_NAME, service);
            return service;
        }
        return (WorkflowStrategyService) registration.getService(SERVICE_NAME);
    }

    /**
     * Get a configured strategy instance
     *
     * @param workflow workflow
     * @param config   config data
     *
     * @return instance with configuration applied
     *
     * @throws ExecutionServiceException if provider cannot be loaded
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
                PluginAdapterUtility.configureProperties(
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
     * @throws ExecutionServiceException if provider cannot be loaded
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
                PluginAdapterUtility.configureProperties(
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

    public void registerClass(String name, Class<? extends WorkflowStrategy> clazz) {
        builtinService.registerClass(name, clazz);
    }

    public void registerInstance(String name, WorkflowStrategy object) {
        builtinService.registerInstance(name, object);
    }
}

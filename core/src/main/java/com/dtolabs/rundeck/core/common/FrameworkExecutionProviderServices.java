package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutor;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.PluggableProviderService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.plugins.ServiceTypes;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;
import lombok.Getter;
import lombok.Setter;
import org.rundeck.core.plugins.PluginProviderServices;

/**
 * Implement PluginProviderServices by accessing the framework execution provider services
 */
public class FrameworkExecutionProviderServices
        implements PluginProviderServices

{
    @Getter @Setter private IExecutionServices frameworkExecutionServices;

    @Override
    public <T> boolean hasServiceFor(final Class<T> serviceType, final String serviceName) {
        Class<?> aClass = ServiceTypes.EXECUTION_TYPES.get(serviceName);
        return aClass !=null && aClass.equals(serviceType);
    }

    @Override
    public <T> PluggableProviderService<T> getServiceProviderFor(
            final Class<T> serviceType,
            final String serviceName,
            final ServiceProviderLoader loader
    )
    {
        if(serviceType.equals(NodeExecutor.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getNodeExecutorService();
        }else if(serviceType.equals(FileCopier.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getFileCopierService();
        }else if(serviceType.equals(WorkflowStrategy.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getWorkflowStrategyService();
        }else if(serviceType.equals(StepExecutor.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getStepExecutionService();
        }else if(serviceType.equals(NodeStepExecutor.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getNodeStepExecutorService();
        }else if(serviceType.equals(ResourceModelSourceFactory.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getResourceModelSourceService();
        }else if(serviceType.equals(ResourceFormatGenerator.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getResourceFormatGeneratorService();
        }else if(serviceType.equals(ResourceFormatParser.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getResourceFormatParserService();
        }else if(serviceType.equals(WorkflowExecutor.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getWorkflowExecutionService();
        }else if(serviceType.equals(OrchestratorPlugin.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getOrchestratorService();
        }else if(serviceType.equals(NodeDispatcher.class)){
            return (PluggableProviderService<T>) frameworkExecutionServices.getNodeDispatcherService();
        }
        return null;
    }
}

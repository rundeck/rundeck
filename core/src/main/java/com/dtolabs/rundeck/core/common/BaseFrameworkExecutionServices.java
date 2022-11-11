package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.FileCopierService;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

public class BaseFrameworkExecutionServices
        implements IExecutionServicesRegistration
{
    final HashMap<String, FrameworkSupportService> services = new HashMap<String, FrameworkSupportService>();

    @Getter @Setter private Framework framework;

    public static BaseFrameworkExecutionServices create(final Framework framework) {
        BaseFrameworkExecutionServices impl = new BaseFrameworkExecutionServices();
        impl.setFramework(framework);
        return impl;
    }

    /**
     * @param name service name
     * @return a service by name
     */
    @Override
    public FrameworkSupportService getService(String name) {
        return services.get(name);
    }

    /**
     * Set a service by name
     *
     * @param name    name
     * @param service service
     */
    @Override
    public void setService(final String name, final FrameworkSupportService service) {
        synchronized (services) {
            if (null == services.get(name) && null != service) {
                services.put(name, service);
            } else if (null == service) {
                services.remove(name);
            }
        }
    }

    @Override
    public void overrideService(final String name, final FrameworkSupportService service) {
        synchronized (services) {
            services.put(name, service);
        }
    }

    @Override
    public OrchestratorService getOrchestratorService() {
        return OrchestratorService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return WorkflowExecutionService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public WorkflowStrategyService getWorkflowStrategyService() {
        return WorkflowStrategyService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public StepExecutionService getStepExecutionService() {
        return StepExecutionService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return FileCopierService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public NodeExecutorService getNodeExecutorService() {
        return NodeExecutorService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public NodeStepExecutionService getNodeStepExecutorService() {
        return NodeStepExecutionService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public NodeDispatcherService getNodeDispatcherService() {
        return NodeDispatcherService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public ResourceModelSourceService getResourceModelSourceService() {
        return ResourceModelSourceService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return ResourceFormatParserService.getInstanceForFramework(getFramework(), this);
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return ResourceFormatGeneratorService.getInstanceForFramework(getFramework(), this);
    }

}

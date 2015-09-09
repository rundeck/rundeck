package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.ExecutionServiceFactory;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.PluginManagerService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.HashMap;

/**
 * Created by greg on 2/20/15.
 */
public class ServiceSupport implements IFrameworkServices {
    public static final String FRAMEWORK_PLUGINS_ENABLED = "framework.plugins.enabled";

    final HashMap<String,FrameworkSupportService> services = new HashMap<String, FrameworkSupportService>();

    private Framework framework;

    public ServiceSupport() {

    }

    /**
     * Initialize children, the various resource management objects
     */
    public void initialize(Framework framework) {
        setFramework(framework);
        //plugin manager service inited first.  any pluggable services will then be
        //able to try to load providers via the plugin manager
        if(!framework.hasProperty(FRAMEWORK_PLUGINS_ENABLED) || "true".equals(framework.getProperty(FRAMEWORK_PLUGINS_ENABLED))){
            //enable plugin service only if framework property does not disable them
            PluginManagerService.getInstanceForFramework(getFramework());
        }
        NodeStepExecutionService.getInstanceForFramework(getFramework());
        NodeExecutorService.getInstanceForFramework(getFramework());
        FileCopierService.getInstanceForFramework(getFramework());
        NodeDispatcherService.getInstanceForFramework(getFramework());
        getExecutionService();
        WorkflowExecutionService.getInstanceForFramework(getFramework());
        StepExecutionService.getInstanceForFramework(getFramework());
        ResourceModelSourceService.getInstanceForFramework(getFramework());
        ResourceFormatParserService.getInstanceForFramework(getFramework());
        ResourceFormatGeneratorService.getInstanceForFramework(getFramework());
    }

    /**
     * @return a service by name
     * @param name service name
     */
    @Override
    public FrameworkSupportService getService(String name) {
        return services.get(name);
    }
    /**
     * Set a service by name
     * @param name name
     * @param service service
     */
    @Override
    public void setService(final String name, final FrameworkSupportService service){
        synchronized (services){
            if(null==services.get(name) && null!=service) {
                services.put(name, service);
            }else if(null==service) {
                services.remove(name);
            }
        }
    }

    @Override
    public OrchestratorService getOrchestratorService() {
        return OrchestratorService.getInstanceForFramework(getFramework());
    }

    @Override
    public ExecutionService getExecutionService() {
        return ExecutionServiceFactory.getInstanceForFramework(getFramework());
    }
    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return WorkflowExecutionService.getInstanceForFramework(getFramework());
    }
    @Override
    public StepExecutionService getStepExecutionService() {
        return StepExecutionService.getInstanceForFramework(getFramework());
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(INodeEntry node, final String project) throws
            ExecutionServiceException
    {
        return getFileCopierService().getProviderForNodeAndProject(node, project);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return FileCopierService.getInstanceForFramework(getFramework());
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return getNodeExecutorService().getProviderForNodeAndProject(node, project);
    }
    @Override
    public NodeExecutorService getNodeExecutorService() throws ExecutionServiceException {
        return NodeExecutorService.getInstanceForFramework(getFramework());
    }
    @Override
    public NodeStepExecutionService getNodeStepExecutorService() throws ExecutionServiceException {
        return NodeStepExecutionService.getInstanceForFramework(getFramework());
    }
    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(NodeStepExecutionItem item) throws ExecutionServiceException {
        return NodeStepExecutionService.getInstanceForFramework(getFramework()).getExecutorForExecutionItem(item);
    }
    @Override
    public NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException {
        return NodeDispatcherService.getInstanceForFramework(getFramework()).getNodeDispatcher(context);
    }
    @Override
    public ResourceModelSourceService getResourceModelSourceService() {
        return ResourceModelSourceService.getInstanceForFramework(getFramework());
    }

    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return ResourceFormatParserService.getInstanceForFramework(getFramework());
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return ResourceFormatGeneratorService.getInstanceForFramework(getFramework());
    }

    @Override
    public ServiceProviderLoader getPluginManager(){
        if(null!=getService(PluginManagerService.SERVICE_NAME)) {
            return PluginManagerService.getInstanceForFramework(getFramework());
        }
        return null;
    }


    public Framework getFramework() {
        return framework;
    }

    public void setFramework(final Framework framework) {
        this.framework = framework;
    }
}

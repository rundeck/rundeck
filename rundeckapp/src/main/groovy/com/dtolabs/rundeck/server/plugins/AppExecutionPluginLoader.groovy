package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IExecutionProviders
import com.dtolabs.rundeck.core.common.IFrameworkNodes
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierService
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.execution.service.NodeProviderName
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepPluginAdapter
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepPluginAdapter
import com.dtolabs.rundeck.core.plugins.PluginConfigureService
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.StepPlugin
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * <p>This class implements IExecutionProviders needed by Framework service support,
 * by loading execution providers in several ways:</p>
 * <ol>
 * <li>if the "rundeckBaseFrameworkExecutionProviders" bean contains has a registration for
 * the provider, it is loaded that way. This is how "built-in" providers are currently loaded.</li>
 * <li>In two cases (StepExecutor,NodeStepExecutor), if the app-level "pluginService" has a registration for the
 * provider, it is loaded that way.  This is how JobReference feature is implemented, because it does not
 * implement the actual Plugin interface types.  See the "JobReferenceNodeStepExecutor" for example.</li>
 * <li>If the actual Plugin interface type has a registration in the app layer, that is used.</li>
 * </ol>
 * <p>
 *     This allows loading plugin providers necessary for workflows/executions, which are accessed via the central Framework/IFrameworkProviders,
 *     via the app-level configuration mechanism.
 * </p>
 */
@CompileStatic
class AppExecutionPluginLoader implements IExecutionProviders, ApplicationContextAware {
    @Autowired PluginConfigureService pluginService
    @Autowired IFrameworkNodes rundeckNodeSupport
    @Autowired NodeProviderName nodeProviderName
    private Framework rundeckFramework
    private IExecutionProviders rundeckBaseFrameworkExecutionProviders
    ApplicationContext applicationContext

    //lazy load the framework
    private Framework getFramework() throws Exception {
        if (null == rundeckFramework) {
            rundeckFramework = applicationContext.getBean('rundeckFramework', Framework)
        }
        return rundeckFramework
    }

    private IExecutionProviders getFrameworkProviders() {
        if (null == rundeckBaseFrameworkExecutionProviders) {
            rundeckBaseFrameworkExecutionProviders = applicationContext
                .getBean('rundeckBaseFrameworkExecutionProviders', IExecutionProviders)
        }
        return rundeckBaseFrameworkExecutionProviders
    }

    @Override
    StepExecutor getStepExecutorForItem(final StepExecutionItem item, String project) throws ExecutionServiceException {
        //check static predefined proivder
        if (StepExecutionService.isRegistered(item.type)) {
            return frameworkProviders.getStepExecutorForItem(item, project)
        }
        Map<String, Object> config = new HashMap<>()
        if (item instanceof ConfiguredStepExecutionItem) {
            config = ((ConfiguredStepExecutionItem) item).stepConfiguration
        }
        //app-level registered
        def hasProvider = pluginService.hasRegisteredProvider(item.type, StepExecutor)
        if(hasProvider) {
            def stepExecConfigured = pluginService.configurePlugin(item.type, config, project, framework, StepExecutor)
            if (stepExecConfigured) {
                return stepExecConfigured.instance
            }
        }

        //plugin adapater
        def configured = pluginService.configurePlugin(item.type, config, project, framework, StepPlugin)
        if (null == configured) {
            throw new ExecutionServiceException("Could not load Step provider: ${item.type}: not found")
        }
        StepPlugin plugin = configured.instance
        return StepPluginAdapter.CONVERTER.convert(plugin)
    }

    @Override
    FileCopier getFileCopierForNodeAndProject(final INodeEntry node, final ExecutionContext context)
        throws ExecutionServiceException {

        String copiername = nodeProviderName.getProviderNameForNodeAndProject(node, context.getFrameworkProject(), FileCopier)

        if (FileCopierService.isRegistered(copiername)) {
            return frameworkProviders.getFileCopierForNodeAndProject(node, context);
        }

        def configured = pluginService.configurePlugin(copiername, [:], context.getFrameworkProject(), framework, FileCopier)
        if (null == configured) {
            throw new ExecutionServiceException("Could not load FileCopier provider: ${copiername}: not found")
        }
        return configured.instance
    }

    @Override
    NodeExecutor getNodeExecutorForNodeAndProject(final INodeEntry node, final ExecutionContext context)
        throws ExecutionServiceException {
        String provider = nodeProviderName.getProviderNameForNodeAndProject(node, context.getFrameworkProject(), NodeExecutor)

        if (NodeExecutorService.isRegistered(provider)) {
            return frameworkProviders.getNodeExecutorForNodeAndProject(node,context);
        }

        def configured = pluginService.configurePlugin(provider, [:], context.getFrameworkProject(), framework, NodeExecutor)
        if (null == configured) {
            throw new ExecutionServiceException("Could not load NodeExecutor provider: ${provider}: not found")
        }
        return configured.instance
    }

    @Override
    NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item, String project)
        throws ExecutionServiceException {
        //predefined/registered from core classes
        if (NodeStepExecutionService.isRegistered(item.nodeStepType)) {
            return frameworkProviders.getNodeStepExecutorForItem(item, project);
        }
        Map<String, Object> config = new HashMap<>()
        if (item instanceof ConfiguredStepExecutionItem) {
            config = ((ConfiguredStepExecutionItem) item).stepConfiguration
        }

        def hasProvider = pluginService.hasRegisteredProvider(item.nodeStepType, NodeStepExecutor)
        //app-level registered
        if(hasProvider) {
            def stepExecConfigured = pluginService
                .configurePlugin(item.nodeStepType, config, project, framework, NodeStepExecutor)
            if (stepExecConfigured) {
                return stepExecConfigured.instance
            }
        }

        //plugin adapted
        def configured = pluginService
            .configurePlugin(item.nodeStepType, config, project, framework, NodeStepPlugin)
        if (null == configured) {
            throw new ExecutionServiceException("Could not load Node Step provider: ${item.nodeStepType}: not found")
        }
        NodeStepPlugin plugin = configured.instance
        return NodeStepPluginAdapter.CONVERT_TO_NODE_STEP_EXECUTOR.convert(plugin)
    }


    @Override
    NodeDispatcher getNodeDispatcherForContext(final ExecutionContext context) throws ExecutionServiceException {
        return frameworkProviders.getNodeDispatcherForContext(context);
    }
}

package rundeck.services


import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.ExecutionLifecyclePluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.jobs.IExecutionLifecyclePluginService
import com.dtolabs.rundeck.core.jobs.ExecutionLifecyclePluginHandler
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl

import com.dtolabs.rundeck.server.plugins.services.ExecutionLifecyclePluginProviderService
import groovy.transform.CompileStatic
import rundeck.ScheduledExecution

/**
 * Provides capability to execute certain task based on a job execution event
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */
class ExecutionLifecyclePluginService implements IExecutionLifecyclePluginService {

    PluginService pluginService
    ExecutionLifecyclePluginProviderService executionLifecyclePluginProviderService
    FrameworkService frameworkService
    def featureService

    enum EventType{
        BEFORE_RUN('beforeJobRun'), AFTER_RUN('afterJobRun')
        private final String value
        EventType(String value){
            this.value = value
        }
        String getValue(){
            this.value
        }
    }

    /**
     *
     * @return Map containing all of the ExecutionLifecyclePlugin implementations
     */
    Map listExecutionLifecyclePlugins(){
        if(featureService?.featurePresent('executionLifecyclePlugin', false)){
            return pluginService?.listPlugins(ExecutionLifecyclePlugin)
        }
        return null
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @return ExecutionLifecycleStatus response from plugin implementation
     */
    ExecutionLifecycleStatus handleEvent(def event, EventType eventType, List<NamedExecutionLifecyclePlugin> plugins) throws ExecutionLifecyclePluginException{
        if (!plugins) {
            return null
        }
        def errors = [:]
        def results = [:]
        Exception firstErr
        ExecutionLifecycleStatus prevResult = null
        def prevEvent = event
        boolean success = true
        for (NamedExecutionLifecyclePlugin plugin : plugins) {
            try {

                def curEvent = mergeEvent(prevResult, prevEvent)
                ExecutionLifecycleStatus result = handleEventForPlugin(eventType, plugin, curEvent)
                if (result != null && !result.successful) {
                    success = false
                    log.info("Result from plugin is false an exception will be thrown")
                    if (result.getErrorMessage() != null && !result.getErrorMessage().trim().isEmpty()) {
                        throw new ExecutionLifecyclePluginException(result.getErrorMessage())
                    } else {
                        throw new ExecutionLifecyclePluginException(
                                "Response from $plugin.name is false, but no description was provided by the plugin"
                        )
                    }

                }
                if (result != null && result.isUseNewValues()) {
                    results[plugin.name] = result
                    prevResult = result
                }
                prevEvent = curEvent
            } catch (Exception e) {
                success = false
                if (!firstErr) {
                    firstErr = e
                }
                errors[plugin.name] = e
            }
        }
        if (errors) {
            errors.each { name, Exception e ->
                log.error("Error (ExecutionLifecyclePlugin:$name/$eventType): $e.message", e)
            }
            if (firstErr) {
                throw firstErr
            }
        }

        mergeEventResult(success, prevResult, prevEvent, !results.isEmpty())

    }

    ExecutionLifecycleStatus handleEventForPlugin(
            EventType eventType,
            NamedExecutionLifecyclePlugin plugin,
            event
    ) {
        switch (eventType) {
            case EventType.BEFORE_RUN:
                return plugin.beforeJobStarts(event)
            case EventType.AFTER_RUN:
                return plugin.afterJobEnds(event)
        }
    }

    /**
     * Merge
     * @param status
     * @param jobEvent
     * @return
     */
    Object mergeEvent(final ExecutionLifecycleStatus status, final Object jobEvent) {
        if (jobEvent instanceof JobExecutionEventImpl) {
            ExecutionContextImpl newContext = mergeExecutionEventContext(
                    jobEvent.executionContext,
                    status
            )

            return jobEvent.result != null ?
                   JobExecutionEventImpl.afterRun(newContext, jobEvent.execution, jobEvent.result) :
                   JobExecutionEventImpl.beforeRun(newContext, jobEvent.execution, jobEvent.workflow)
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }


    /**
     * Merge original event, event status result, return a new result
     * @param success overall success
     * @param status result of plugin handling event
     * @param jobEvent event
     * @return result with merged contents for the type of event
     */
    ExecutionLifecycleStatus mergeEventResult(boolean success, final ExecutionLifecycleStatus status, final Object jobEvent, boolean useNewValues) {
        if (jobEvent instanceof JobExecutionEventImpl) {
            ExecutionContextImpl newContext = mergeExecutionEventContext(jobEvent.executionContext, status)

            return new ExecutionLifecycleStatusImpl(successful: success, executionContext: newContext)
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }

    /**
     * Merge the context with result of event
     * @param context original
     * @param status result of event
     * @return merged context if the event has an executionContext value and useNewValues is true
     */
    ExecutionContextImpl mergeExecutionEventContext(
            StepExecutionContext context,
            ExecutionLifecycleStatus status
    ) {
        def newContextBuilder = ExecutionContextImpl.builder(context)
        if (status && status.isUseNewValues() && status.executionContext) {
            newContextBuilder.merge(ExecutionContextImpl.builder(status.executionContext))
        }
        newContextBuilder.build()
    }

    /**
     * Load configured ExecutionLifecyclePlugin instances for the job
     * @param configurations
     * @param project
     * @return
     */
    List<NamedExecutionLifecyclePlugin> createConfiguredPlugins(PluginConfigSet configurations, String project) {
        List<NamedExecutionLifecyclePlugin> configured = []

        configurations?.pluginProviderConfigs?.each { PluginProviderConfiguration pluginConfig ->
            String type = pluginConfig.provider
            def configuredPlugin = pluginService.configurePlugin(
                    type,
                    pluginConfig.configuration,
                    project,
                    frameworkService.rundeckFramework,
                    ExecutionLifecyclePlugin
            )
            if (!configuredPlugin) {
                //TODO: could not load plugin, or config was invalid
                return
            }
            configured << new NamedExecutionLifecyclePlugin(plugin: (ExecutionLifecyclePlugin) configuredPlugin.instance, name: type)
        }
        configured
    }


    /**
     *
     * @param project
     * @return map of described plugins enabled for the project
     */
    Map<String, DescribedPlugin<ExecutionLifecyclePlugin>> listEnabledExecutionLifecyclePlugins(
            PluginControlService pluginControlService
    ) {
        if (!featureService.featurePresent('executionLifecyclePlugin', false)) {
            return null
        }

        return pluginService.listPlugins(ExecutionLifecyclePlugin).findAll { k, v ->
            !pluginControlService?.isDisabledPlugin(k, ServiceNameConstants.ExecutionLifecycle)
        }
    }

    /**
     * Read the config set for the job
     * @param job
     * @return PluginConfigSet for the ExecutionLifecyclePlugin service for the job, or null if not defined or not enabled
     */
    PluginConfigSet getExecutionLifecyclePluginConfigSetForJob(ScheduledExecution job) {
        if (!featureService?.featurePresent('executionLifecyclePlugin', false)) {
            return null
        }
        def pluginConfig = job.pluginConfigMap?.get ServiceNameConstants.ExecutionLifecycle

        if (!(pluginConfig instanceof Map)) {
            return null
        }
        List<PluginProviderConfiguration> configs = []
        pluginConfig.each { String type, Map config ->
            configs << SimplePluginConfiguration.builder().provider(type).configuration(config).build()
        }

        PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, configs
    }

    /**
     * Store the plugin config set for the job
     * @param job job
     * @param configSet config set
     */
    def setExecutionLifecyclePluginConfigSetForJob(final ScheduledExecution job, final PluginConfigSet configSet) {
        Map<String, Map<String, Object>> data = configSet?.pluginProviderConfigs?.collectEntries {
            [it.provider, it.configuration]
        }
        job.setPluginConfigVal(ServiceNameConstants.ExecutionLifecycle, data)
    }


    /**
     * Create handler for execution ref and plugin configuration
     *
     * @param configurations configurations
     * @param executionReference reference
     * @return execution event handler
     */
    ExecutionLifecyclePluginHandler getExecutionHandler(PluginConfigSet configurations, ExecutionReference executionReference) {
        if (!featureService?.featurePresent('executionLifecyclePlugin', false)) {
            return null
        }
        if (!configurations) {
            return null
        }
        def plugins = createConfiguredPlugins(configurations, executionReference.project)
        new ExecutionReferenceLifecyclePluginHandler(
                executionReference: executionReference,
                executionLifecyclePluginService: this,
                plugins: plugins
        )
    }
}

@CompileStatic
class NamedExecutionLifecyclePlugin implements ExecutionLifecyclePlugin {
    @Delegate ExecutionLifecyclePlugin plugin
    String name
}

@CompileStatic
class ExecutionLifecycleStatusImpl implements ExecutionLifecycleStatus {
    boolean successful
    boolean useNewValues
    StepExecutionContext executionContext
}

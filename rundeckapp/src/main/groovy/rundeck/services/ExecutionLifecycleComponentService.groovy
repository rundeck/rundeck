package rundeck.services

import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.ExecutionLifecycleComponentException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleComponent
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.jobs.IExecutionLifecycleComponentService
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleComponentHandler
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import com.dtolabs.rundeck.server.plugins.services.ExecutionLifecyclePluginProviderService
import grails.events.annotation.Subscriber
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution
import rundeck.services.feature.FeatureService

/**
 * Provides capability to execute certain task based on a job execution event
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */
@CompileStatic
@Slf4j
class ExecutionLifecycleComponentService implements IExecutionLifecycleComponentService, ApplicationContextAware  {

    @Autowired
    ExecutionLifecyclePluginProviderService executionLifecyclePluginProviderService

    @Autowired
    FrameworkService frameworkService

    @Autowired
    FeatureService featureService

    Map<String, ExecutionLifecycleComponent> beanComponents

    //using lazy loader
    PluginService pluginService

    ApplicationContext applicationContext


    @Subscriber('rundeck.bootstrap')
    void init() throws Exception {
        beanComponents = applicationContext.getBeansOfType(ExecutionLifecycleComponent)
    }
    

    //lazy load the pluginService
    private PluginService getPluginService() throws Exception {
        if (null == pluginService) {
            pluginService = applicationContext.getBean('pluginService', PluginService)
        }
        return pluginService
    }


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
        if(!featureService?.featurePresent(Features.EXECUTION_LIFECYCLE_PLUGIN, false)){
            return pluginService?.listPlugins(ExecutionLifecyclePlugin, executionLifecyclePluginProviderService)
        }
        return null
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @return ExecutionLifecycleStatus response from plugin implementation
     */
    ExecutionLifecycleStatus handleEvent(def event, EventType eventType, List<NamedExecutionLifecycleComponent> components) throws ExecutionLifecycleComponentException{
        if (!components) {
            return null
        }
        Map<String, Exception> errors = [:]
        def results = [:]
        Exception firstErr
        ExecutionLifecycleStatus prevResult = null
        def prevEvent = event
        boolean success = true
        for (NamedExecutionLifecycleComponent component : components) {
            try {

                def curEvent = mergeEvent(prevResult, prevEvent)
                ExecutionLifecycleStatus result = handleEventForPlugin(eventType, component, curEvent)
                if (result != null && !result.successful) {
                    success = false
                    log.info("Result from component is false an exception will be thrown")
                    if (result.getErrorMessage() != null && !result.getErrorMessage().trim().isEmpty()) {
                        throw new ExecutionLifecycleComponentException(result.getErrorMessage())
                    } else {
                        throw new ExecutionLifecycleComponentException(
                                "Response from $component.name is false, but no description was provided by the component"
                        )
                    }

                }
                if (result != null && result.isUseNewValues()) {
                    results[component.name] = result
                    prevResult = result
                }
                prevEvent = curEvent
            } catch (Exception e) {
                success = false
                if (!firstErr) {
                    firstErr = e
                }
                errors[component.name] = e
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

    @CompileDynamic
    ExecutionLifecycleStatus handleEventForPlugin(
            EventType eventType,
            NamedExecutionLifecycleComponent plugin,
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
    List<NamedExecutionLifecycleComponent> createConfiguredPlugins(PluginConfigSet configurations, String project) {
        List<NamedExecutionLifecycleComponent> configured = []

        configurations?.pluginProviderConfigs?.each { PluginProviderConfiguration pluginConfig ->
            String type = pluginConfig.provider
            def configuredPlugin = getPluginService().configurePlugin(
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
            configured << new NamedExecutionLifecycleComponent(component: (ExecutionLifecyclePlugin) configuredPlugin.instance, name: type)
        }
        configured
    }

    List<NamedExecutionLifecycleComponent> loadConfiguredComponents(PluginConfigSet configurations, String project) {
        List compList = []
        if(beanComponents){
            List<NamedExecutionLifecycleComponent> namedComponents = beanComponents.collect {name, component->
                new NamedExecutionLifecycleComponent(
                        component: component,
                        name: component.class.canonicalName)
            }

            namedComponents.forEach {component->
                if(!component.isPlugin()){
                    compList.add(component)
                }
            }
        }
        compList
    }


    /**
     *
     * @param project
     * @return map of described plugins enabled for the project
     */
    Map<String, DescribedPlugin<ExecutionLifecyclePlugin>> listEnabledExecutionLifecyclePlugins(
            PluginControlService pluginControlService
    ) {
        if (!featureService.featurePresent(Features.EXECUTION_LIFECYCLE_PLUGIN, false)) {
            return null
        }

        return getPluginService().listPlugins(ExecutionLifecyclePlugin).findAll { k, v ->
            !pluginControlService?.isDisabledPlugin(k, ServiceNameConstants.ExecutionLifecycle)
        }
    }

    /**
     * Read the config set for the job
     * @param job
     * @return PluginConfigSet for the ExecutionLifecyclePlugin service for the job, or null if not defined or not enabled
     */
    @CompileDynamic
    PluginConfigSet getExecutionLifecyclePluginConfigSetForJob(ScheduledExecution job) {
        if (!featureService?.featurePresent(Features.EXECUTION_LIFECYCLE_PLUGIN, false)) {
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
        if(configSet){
            Map<String, Map<String, Object>> data = configSet?.pluginProviderConfigs?.collectEntries {
                [it.provider, it.configuration]
            }
            job.setPluginConfigVal(ServiceNameConstants.ExecutionLifecycle, data)
        }
    }


    /**
     * Create handler for execution ref and plugin configuration
     *
     * @param configurations configurations
     * @param executionReference reference
     * @return execution event handler
     */
    ExecutionLifecycleComponentHandler getExecutionHandler(PluginConfigSet configurations, ExecutionReference executionReference) {
        if (!featureService?.featurePresent(Features.EXECUTION_LIFECYCLE_PLUGIN, false)) {
            return null
        }
        def components = loadConfiguredComponents(configurations, executionReference.project)
        if(configurations){
            components.addAll(createConfiguredPlugins(configurations, executionReference.project))
        }
        new ExecutionReferenceLifecycleComponentHandler(
                executionReference: executionReference,
                executionLifecycleComponentService: this,
                components: components
        )
    }
}

@CompileStatic
class NamedExecutionLifecycleComponent implements ExecutionLifecycleComponent {
    @Delegate ExecutionLifecycleComponent component
    String name
    boolean isPlugin() {
        return component instanceof ExecutionLifecyclePlugin
    }

}

@CompileStatic
class ExecutionLifecycleStatusImpl implements ExecutionLifecycleStatus {
    boolean successful
    boolean useNewValues
    StepExecutionContext executionContext
}

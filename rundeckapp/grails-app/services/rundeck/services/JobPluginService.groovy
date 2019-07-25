package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.IJobPluginService
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPluginExecutionHandler
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPlugin
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl

import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobPluginProviderService
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution

/**
 * Provides capability to execute certain task based on job a job event
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */

public class JobPluginService implements ApplicationContextAware, ProjectConfigurable, IJobPluginService{

    ApplicationContext applicationContext
    PluginService pluginService
    JobPluginProviderService jobPluginProviderService
    FrameworkService frameworkService
    def featureService
    public static final String CONF_PROJECT_ENABLE_JOB = 'project.enable.jobPlugin.'

    Map<String, String> configPropertiesMapping
    Map<String, String> configProperties
    List<Property> projectConfigProperties


    enum EventType{
        PRE_EXECUTION("preExecution"),BEFORE_RUN('beforeJobRun'), AFTER_RUN('afterJobRun'),
        BEFORE_SAVE("beforeSave")
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
     * It loads the plugin properties to be shown under the project configuration
     */
    def loadProperties(){
        List<Property> projectConfigProperties = []
        Map<String, String> configPropertiesMapping = [:]
        Map<String, String> configProperties = [:]
        listJobPlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        booleanType 'jobPlugin' + name
                        title('Enable ' + (describedPlugin.description?.title ?: name))
                        required(false)
                        defaultValue null
                        renderingOption('booleanTrueDisplayValueClass', 'text-warning')
                    }.build()
            )
            configPropertiesMapping.put('jobPlugin' + name , CONF_PROJECT_ENABLE_JOB + name)
            configProperties.put('jobPlugin' + name, 'jobPlugin')
        }
        this.configPropertiesMapping = configPropertiesMapping
        this.configProperties = configProperties
        this.projectConfigProperties = projectConfigProperties
    }

    @Override
    Map<String, String> getCategories() {
        loadProperties()
        configProperties
    }

    @Override
    Map<String, String> getPropertiesMapping() {
        loadProperties()
        configPropertiesMapping
    }

    @Override
    List<Property> getProjectConfigProperties() {
        loadProperties()
        projectConfigProperties
    }

    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    DescribedPlugin getJobPluginDescriptor(String name) {
        return pluginService.getPluginDescriptor(name, jobPluginProviderService)
    }

    /**
     *
     * @return Map containing all of the JobPlugin implementations
     */
    Map listJobPlugins(){
        if(featureService?.featurePresent('job-plugin', false)){
            return pluginService?.listPlugins(JobPlugin, jobPluginProviderService)
        }
        return null
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobExecution(ScheduledExecution job, JobPreExecutionEvent event) {
        def plugins = createConfiguredPlugins(getJobPluginConfigSetForJob(job), job.project)
        handleEvent(event, EventType.PRE_EXECUTION, plugins)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobSave(ScheduledExecution job, JobPersistEvent event) {
        def plugins = createConfiguredPlugins(getJobPluginConfigSetForJob(job), job.project)
        handleEvent(event, EventType.BEFORE_SAVE, plugins)
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus handleEvent(def event, EventType eventType, List<NamedJobPlugin> plugins) {
        if (!plugins) {
            return null
        }
        def errors = [:]
        def results = [:]
        Exception firstErr
        JobEventStatus prevResult = null
        def prevEvent = event
        boolean success = true
        for (NamedJobPlugin plugin : plugins) {
            try {

                def curEvent = mergeEvent(prevResult, prevEvent)
                def JobEventStatus result = handleEventForPlugin(eventType, plugin, curEvent)
                if (result != null && !result.successful) {
                    success = false
                    log.info("Result from plugin is false an exception will be thrown")
                    if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
                        throw new JobPluginException(result.getDescription())
                    } else {
                        throw new JobPluginException(
                                "Response from $plugin.name is false, but no description was provided by the plugin"
                        )
                    }

                }
                if (result != null) {
                    results[plugin.name] = result
                }
                prevResult = result
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
                log.error("Error (JobPlugin:$name/$eventType): $e.message", e)
            }
            if (firstErr) {
                throw firstErr
            }
        }

        mergeEventResult(success, prevResult, prevEvent)
    }

    public JobEventStatus handleEventForPlugin(
            EventType eventType,
            NamedJobPlugin plugin,
            event
    ) {
        switch (eventType) {
            case EventType.BEFORE_RUN:
                return plugin.beforeJobStarts(event)
            case EventType.AFTER_RUN:
                return plugin.afterJobEnds(event)
            case EventType.PRE_EXECUTION:
                return plugin.beforeJobExecution(event)
            case EventType.BEFORE_SAVE:
                return plugin.beforeSaveJob(event)
        }
    }

    /**
     * Merge
     * @param jobEventStatus
     * @param jobEvent
     * @return
     */
    Object mergeEvent(final JobEventStatus jobEventStatus, final Object jobEvent) {
        if (jobEvent instanceof JobPreExecutionEventImpl) {
            HashMap<String, String> newOptionsValues = mergePreExecutionOptionsValues(
                    jobEvent.optionsValues,
                    jobEventStatus
            )
            return new JobPreExecutionEventImpl(
                    jobEvent.projectName,
                    jobEvent.userName,
                    jobEvent.scheduledExecutionMap,
                    newOptionsValues,
                    jobEvent.nodes
            )
        } else if (jobEvent instanceof JobPersistEvent) {
            TreeSet<JobOption> options = mergePersistOptions(jobEvent.options, jobEventStatus)
            def newEvent = new JobPersistEventImpl(jobEvent)
            newEvent.setNewOptions(options)
            return newEvent
        } else if (jobEvent instanceof JobExecutionEventImpl) {
            ExecutionContextImpl newContext = mergeExecutionEventContext(
                    jobEvent.executionContext,
                    jobEventStatus
            )

            return new JobExecutionEventImpl(newContext, jobEvent.execution, jobEvent.result)
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }


    /**
     * Merge original event, event status result, return a new result
     * @param success overall success
     * @param jobEventStatus result of plugin handling event
     * @param jobEvent event
     * @return result with merged contents for the type of event
     */
    JobEventStatus mergeEventResult(boolean success, final JobEventStatus jobEventStatus, final Object jobEvent) {
        if (jobEvent instanceof JobPreExecutionEventImpl) {
            HashMap<String, String> newOptionsValues = mergePreExecutionOptionsValues(
                    jobEvent.optionsValues,
                    jobEventStatus
            )
            return new JobEventStatusImpl(
                    successful: success,
                    optionsValues: newOptionsValues
            )
        } else if (jobEvent instanceof JobPersistEvent) {
            TreeSet<JobOption> options = mergePersistOptions(jobEvent.options, jobEventStatus)
            return new JobEventStatusImpl(
                    successful: success,
                    options: options
            )
        } else if (jobEvent instanceof JobExecutionEventImpl) {
            ExecutionContextImpl newContext = mergeExecutionEventContext(jobEvent.executionContext, jobEventStatus)

            return new JobEventStatusImpl(successful: success, executionContext: newContext)
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }
    /**
     * Merge the context with result of event
     * @param context original
     * @param jobEventStatus result of event
     * @return merged context if the event has an executionContext value and useNewValues is true
     */
    public ExecutionContextImpl mergeExecutionEventContext(
            StepExecutionContext context,
            JobEventStatus jobEventStatus
    ) {
        def newContextBuilder = ExecutionContextImpl.builder(context)
        if (jobEventStatus && jobEventStatus.useNewValues() && jobEventStatus.executionContext) {
            newContextBuilder.merge(ExecutionContextImpl.builder(jobEventStatus.executionContext))
        }
        newContextBuilder.build()
    }

    /**
     * Merge optionsValues map from event result if useNewValues is true and optionsValues is set
     * @param optionsValues original optionsValues map, or null
     * @param jobEventStatus result of pre execution event
     * @return new map with merged optionsvalues
     */
    public HashMap<String, String> mergePreExecutionOptionsValues(
            Map<String, String> optionsValues,
            JobEventStatus jobEventStatus
    ) {
        def newOptionsValues = new HashMap<String, String>(optionsValues ?: [:])
        if (jobEventStatus && jobEventStatus.useNewValues() && jobEventStatus.optionsValues) {
            newOptionsValues.putAll(jobEventStatus.optionsValues)
        }
        newOptionsValues
    }

    /**
     * Merge initial JobOption set with result of event if useNewValues is specified, or return null if
     * the initial set was null and result value was null or not useNewValues was not set
     * @param initial initial set, or null
     * @param jobEventStatus result of event
     * @return merged set, or null
     */
    public TreeSet<JobOption> mergePersistOptions(SortedSet<JobOption> initial, JobEventStatus jobEventStatus) {
        SortedSet<JobOption> options = initial ? new TreeSet<JobOption>(initial) : null

        if (jobEventStatus && jobEventStatus.useNewValues() && jobEventStatus.options) {
            if (options) {
                options.addAll(jobEventStatus.options)
            } else {
                options = new TreeSet<>(jobEventStatus.options)
            }
        }
        options
    }

    /**
     * It merges the original event with the new ones from the plugins
     * @param event original job event
     * @param optionValuesMap a map containing the instances sent to the plugins (mapped by plugin name)
     */
    private mergeResult(Map instanceMap, eventType, result){
        if(!instanceMap.isEmpty()){
            if(eventType == EventType.PRE_EXECUTION){
                instanceMap.each { String pluginName, JobEventStatus resultFromMap ->
                    if(result == null){
                        result = resultFromMap
                    }else{
                        resultFromMap.getOptionsValues()?.each { String key, String value ->
                            result.getOptionsValues().put(key, value)
                        }
                    }

                }
            }
            else if(eventType == EventType.BEFORE_SAVE){
                SortedSet options = new TreeSet()
                instanceMap.each { String pluginName, JobEventStatus resultFromMap ->
                    if(result == null){
                        result = resultFromMap
                    }
                    resultFromMap.getOptions().each {
                        options.add(it)
                    }
                }
                result.options = options
            }
        }
    }

    /**
     * Load configured JobPlugin instances for the job
     * @param configurations
     * @param project
     * @return
     */
    List<NamedJobPlugin> createConfiguredPlugins(PluginConfigSet configurations, String project) {
        IRundeckProject rundeckProject = frameworkService?.getFrameworkProject(project)
        List<NamedJobPlugin> configured = []
        configurations?.pluginProviderConfigs?.each { PluginProviderConfiguration pluginConfig ->
            String type = pluginConfig.provider
            if (!isProjectJobPluginEnabled(rundeckProject, type)) {
                return
            }
            def configuredPlugin = pluginService.configurePlugin(
                    type,
                    pluginConfig.configuration,
                    project,
                    frameworkService.rundeckFramework,
                    JobPlugin
            )
            if (!configuredPlugin) {
                //could not load plugin
                return
            }
            configured << new NamedJobPlugin(plugin: (JobPlugin) configuredPlugin.instance, name: type)
        }
        configured
    }

    /**
     *
     * @param project
     * @return map of described plugins enabled for the project
     */
    Map<String, DescribedPlugin<JobPlugin>> listEnabledJobPlugins(IRundeckProject project) {
        if (!featureService.featurePresent('job-plugin')) {
            return null
        }
        listEnabledJobPlugins(project, frameworkService.getPluginControlService(project.name))
    }

    /**
     *
     * @param project
     * @return map of described plugins enabled for the project
     */
    Map<String, DescribedPlugin<JobPlugin>> listEnabledJobPlugins(
            IRundeckProject project,
            PluginControlService pluginControlService
    ) {
        if (!featureService.featurePresent('job-plugin')) {
            return null
        }

        return pluginService.listPlugins(JobPlugin).findAll { k, v ->
            isProjectJobPluginEnabled(project, k) &&
            !pluginControlService?.isDisabledPlugin(k, ServiceNameConstants.JobPlugin)
        }
    }

    /**
     *
     * @param rundeckProject
     * @param provider JobPlugin provider name
     * @return true if the job plugin name is enabled for the project
     */
    public boolean isProjectJobPluginEnabled(IRundeckProject rundeckProject, String provider) {
        rundeckProject?.hasProperty(CONF_PROJECT_ENABLE_JOB + provider) &&
        rundeckProject?.getProperty(CONF_PROJECT_ENABLE_JOB + provider) == "true"
    }

    /**
     * Read the config set for the job
     * @param job
     * @return PluginConfigSet for the JobPlugin service for the job, or null if not defined or not enabled
     */
    PluginConfigSet getJobPluginConfigSetForJob(ScheduledExecution job) {
        if (!featureService?.featurePresent('job-plugin', false)) {
            return null
        }
        def jobPluginConfig = job.pluginConfigMap?.get ServiceNameConstants.JobPlugin

        if (!(jobPluginConfig instanceof Map)) {
            return null
        }
        List<PluginProviderConfiguration> configs = []
        jobPluginConfig.each { String type, Map config ->
            configs << SimplePluginConfiguration.builder().provider(type).configuration(config).build()
        }

        PluginConfigSet.with ServiceNameConstants.JobPlugin, configs
    }

    /**
     * Store the plugin config set for the job
     * @param job job
     * @param configSet config set
     */
    def setJobPluginConfigSetForJob(final ScheduledExecution job, final PluginConfigSet configSet) {
        Map<String, Map<String, Object>> data = configSet.pluginProviderConfigs.collectEntries {
            [it.provider, it.configuration]
        }
        job.setPluginConfigVal(ServiceNameConstants.JobPlugin, data)
    }


    /**
     * Create handler for execution ref and plugin configuration
     *
     * @param configurations configurations
     * @param executionReference reference
     * @return execution event handler
     */
    JobPluginExecutionHandler getExecutionHandler(PluginConfigSet configurations, ExecutionReference executionReference) {
        if (!featureService?.featurePresent('job-plugin', false)) {
            return null
        }
        if (!configurations) {
            return null
        }
        def plugins = createConfiguredPlugins(configurations, executionReference.project)
        new ExecutionReferenceJobPluginHandler(
                executionReference: executionReference,
                jobPluginService: this,
                plugins: plugins
        )
    }
}

class NamedJobPlugin implements JobPlugin {
    @Delegate JobPlugin plugin
    String name
}

class JobEventStatusImpl implements JobEventStatus {
    boolean successful
    Map optionsValues

    @Override
    boolean useNewValues() {
        optionsValues
    }
    StepExecutionContext executionContext
    SortedSet<JobOption> options
}

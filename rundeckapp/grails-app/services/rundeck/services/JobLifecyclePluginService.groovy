package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobLifecyclePluginProviderService
import groovy.transform.CompileStatic
import org.rundeck.core.projects.ProjectConfigurable
import rundeck.ScheduledExecution

/**
 * Provides capability to execute certain based on an event
 * Created by rnavarro
 * Date: 8/23/19
 * Time: 10:37 AM
 */
class JobLifecyclePluginService implements ProjectConfigurable {

    PluginService pluginService
    FrameworkService frameworkService
    def featureService
    JobLifecyclePluginProviderService jobLifecyclePluginProviderService
    public static final String CONF_PROJECT_ENABLED = 'project.enable.jobLifecyclePlugin.'

    Map<String, String> configPropertiesMapping
    Map<String, String> configProperties
    List<Property> projectConfigProperties

    enum EventType{
        PRE_EXECUTION("preExecution"), BEFORE_SAVE("beforeSave")
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
        listJobLifecyclePlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        booleanType 'jobLifecyclePlugin.' + name
                        title( (describedPlugin.description?.title ?: name))
                        description(describedPlugin.description?.description)
                        required(false)
                        defaultValue null
                        renderingOptions( ['icon:plugin:serviceName': ServiceNameConstants.JobLifecycle,'icon:plugin:provider':name])
                    }.build()
            )
            configPropertiesMapping.put('jobLifecyclePlugin.' + name , CONF_PROJECT_ENABLED + name)
            configProperties.put('jobLifecyclePlugin.' + name, 'jobLifecyclePlugin')
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
     * @return Map containing all of the JobLifecyclePlugin implementations
     */
    Map listJobLifecyclePlugins(){
        if(featureService?.featurePresent(Features.JOB_LIFECYCLE_PLUGIN, false)){
            return pluginService?.listPlugins(JobLifecyclePlugin, jobLifecyclePluginProviderService)
        }
        return null
    }

    /**
     * It triggers before the scheduled job is executed
     * @param job current ScheduledExecution
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobLifecycleStatus beforeJobExecution(ScheduledExecution job, JobPreExecutionEvent event)
            throws JobLifecyclePluginException {
        def plugins = createConfiguredPlugins(job.project)
        handleEvent(event, EventType.PRE_EXECUTION, plugins)
    }

    /**
     * It triggers before the scheduled job is saved
     * @param job current ScheduledExecution
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobLifecycleStatus beforeJobSave(ScheduledExecution job, JobPersistEvent event) throws JobLifecyclePluginException {
        def plugins = createConfiguredPlugins(job.project)
        handleEvent(event, EventType.BEFORE_SAVE, plugins)
    }

    /**
     * Load configured JobLifecyclePlugin instances
     * @param project
     * @return
     */
    List<NamedJobLifecyclePlugin> createConfiguredPlugins(String project) {
        List<NamedJobLifecyclePlugin> configured = []
        def rundeckProject = frameworkService.getFrameworkProject(project)
        def defaultPluginTypes = new HashSet<String>(getProjectDefaultJobLifecyclePlugins(rundeckProject))

        defaultPluginTypes.each{type->
            def configuredPlugin = pluginService.configurePlugin(
                    type,
                    [:],
                    project,
                    frameworkService.rundeckFramework,
                    JobLifecyclePlugin
            )
            if (!configuredPlugin) {
                //TODO: could not load plugin, or config was invalid
                return
            }
            configured << new NamedJobLifecyclePlugin(plugin: (JobLifecyclePlugin) configuredPlugin.instance, name: type)
        }
        configured
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @param plugins list of NamedJobLifecyclePlugin
     * @return JobEventStatus response from plugin implementation
     */
    JobLifecycleStatus handleEvent(def event, EventType eventType, List<NamedJobLifecyclePlugin> plugins)
            throws JobLifecyclePluginException {
        if (!plugins) {
            return null
        }
        def errors = [:]
        def results = [:]
        Exception firstErr
        JobLifecycleStatus prevResult = null
        def prevEvent = event
        boolean success = true
        for (NamedJobLifecyclePlugin plugin : plugins) {
            try {

                def curEvent = mergeEvent(prevResult, prevEvent)
                JobLifecycleStatus result = handleEventForPlugin(eventType, plugin, curEvent)
                if (result != null && !result.successful) {
                    success = false
                    if (result != null && result.getErrorMessage() != null && !result.getErrorMessage().trim().isEmpty()) {
                        throw new JobLifecyclePluginException(result.getErrorMessage())
                    } else {
                        throw new JobLifecyclePluginException(
                                "Job Lifecycle Plugin: $plugin.name: result for $eventType: was unsuccessful: no message"
                        )
                    }

                }
                if (result != null && result.isUseNewValues()) {
                    results[plugin.name] = result
                    prevResult = result
                }
                prevEvent = curEvent
            } catch (Throwable e) {
                success = false
                if (!firstErr) {
                    firstErr = e
                }
                errors[plugin.name] = e
            }
        }
        if (errors) {
            errors.each { name, Throwable e ->
                log.debug("Error (JobLifecyclePlugin:$name/$eventType): $e.message", e)
                log.warn("Error (JobLifecyclePlugin:$name/$eventType): $e.message")
            }
            if (firstErr) {
                throw firstErr
            }
        }

        return mergeEventResult(success, prevResult, prevEvent, !results.isEmpty())
    }

    /**
     * Merge original event, event status result, return a new result
     * @param success overall success
     * @param jobEventStatus result of plugin handling event
     * @param jobEvent event
     * @return result with merged contents for the type of event
     */
    JobLifecycleStatus mergeEventResult(boolean success, final JobLifecycleStatus jobEventStatus, final Object jobEvent, boolean useNewValues) {
        if (jobEvent instanceof JobPreExecutionEvent) {
            HashMap<String, String> newOptionsValues = mergePreExecutionOptionsValues(
                    jobEvent.optionsValues,
                    jobEventStatus
            )
            return new JobEventStatusImpl(
                    successful: success,
                    optionsValues: newOptionsValues,
                    useNewValues: useNewValues
            )
        } else if (jobEvent instanceof JobPersistEvent) {
            TreeSet<JobOption> options = mergePersistOptions(jobEvent.options, jobEventStatus)
            return new JobEventStatusImpl(
                    successful: success,
                    options: options,
                    useNewValues: useNewValues
            )
        }else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }

    /**
     * Merge
     * @param jobEventStatus
     * @param jobEvent
     * @return
     */
    Object mergeEvent(final JobLifecycleStatus jobEventStatus, final Object jobEvent) {
        if (jobEvent instanceof JobPreExecutionEvent) {
            HashMap<String, String> newOptionsValues = mergePreExecutionOptionsValues(
                    jobEvent.optionsValues,
                    jobEventStatus
            )
            return new JobPreExecutionEventImpl(
                    jobEvent.getJobName(),
                    jobEvent.projectName,
                    jobEvent.userName,
                    newOptionsValues,
                    jobEvent.nodes,
                    jobEvent.nodeFilter,
                    jobEvent.getOptions()
            )
        } else if (jobEvent instanceof JobPersistEvent) {
            TreeSet<JobOption> options = mergePersistOptions(jobEvent.options, jobEventStatus)
            def newEvent = new JobPersistEventImpl(jobEvent)
            newEvent.setOptions(options)
            return newEvent
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }

    JobLifecycleStatus handleEventForPlugin(
            EventType eventType,
            NamedJobLifecyclePlugin plugin,
            event
    ) {
        switch (eventType) {
            case EventType.PRE_EXECUTION:
                return plugin.beforeJobExecution(event)
            case EventType.BEFORE_SAVE:
                return plugin.beforeSaveJob(event)
        }
    }

    /**
     * Merge optionsValues map from event result if useNewValues is true and optionsValues is set
     * @param optionsValues original optionsValues map, or null
     * @param jobEventStatus result of pre execution event
     * @return new map with merged optionsvalues
     */
    HashMap<String, String> mergePreExecutionOptionsValues(
            Map<String, String> optionsValues,
            JobLifecycleStatus jobEventStatus
    ) {
        def newOptionsValues = new HashMap<String, String>(optionsValues ?: [:])
        if (jobEventStatus && jobEventStatus.isUseNewValues() && jobEventStatus.optionsValues) {
            newOptionsValues.putAll(jobEventStatus.optionsValues)
        }
        newOptionsValues
    }

    /**
     * It replaces initial JobOption set with result of event if useNewValues is specified, or return null if
     * the initial set was null and result value was null or useNewValues was not set
     * @param initial initial set, or null
     * @param jobEventStatus result of event
     * @return merged set, or null
     */
    TreeSet<JobOption> mergePersistOptions(SortedSet<JobOption> initial, JobLifecycleStatus jobEventStatus) {
        SortedSet<JobOption> options = initial ? new TreeSet<JobOption>(initial) : null

        if (jobEventStatus && jobEventStatus.isUseNewValues()) {
            if(jobEventStatus.options){
                options = new TreeSet<>(jobEventStatus.options)
            }else{
                options = null
            }
        }
        options
    }

    /**
     *
     * @param rundeckProject
     * @return Set<String> of enabled JobLifecyclePlugin at project level
     */
    Set<String> getProjectDefaultJobLifecyclePlugins(IRundeckProject rundeckProject) {
        rundeckProject.getProjectProperties().findAll {
            it.key.startsWith(CONF_PROJECT_ENABLED) && it.value == 'true'
        }.collect {
            it.key.substring(CONF_PROJECT_ENABLED.length())
        }
    }


}

@CompileStatic
class NamedJobLifecyclePlugin implements JobLifecyclePlugin {
    @Delegate JobLifecyclePlugin plugin
    String name
}

@CompileStatic
class JobEventStatusImpl implements JobLifecycleStatus {
    boolean successful
    Map optionsValues
    boolean useNewValues
    SortedSet<JobOption> options
}

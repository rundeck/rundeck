package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ProjectPluginException
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl
import com.dtolabs.rundeck.plugins.project.ProjectPlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.ProjectPluginProviderService
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution

/**
 * Provides capability to execute certain based on an event
 * Created by rnavarro
 * Date: 8/23/19
 * Time: 10:37 AM
 */
class ProjectPluginService implements ProjectConfigurable {

    PluginService pluginService
    FrameworkService frameworkService
    def featureService
    ProjectPluginProviderService projectPluginProviderService
    public static final String CONF_PROJECT_ENABLED = 'project.enable.projectPlugin.'

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
        listProjectPlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        booleanType 'projectPlugin.' + name
                        title('Enable ' + (describedPlugin.description?.title ?: name))
                        required(false)
                        defaultValue null
                        renderingOption('booleanTrueDisplayValueClass', 'text-warning')
                    }.build()
            )
            configPropertiesMapping.put('projectPlugin.' + name , CONF_PROJECT_ENABLED + name)
            configProperties.put('projectPlugin.' + name, 'projectPlugin')
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
     * @return Map containing all of the ProjectPlugin implementations
     */
    Map listProjectPlugins(){
        if(featureService?.featurePresent('project-plugin', false)){
            return pluginService?.listPlugins(ProjectPlugin, projectPluginProviderService)
        }
        return null
    }

    /**
     * It triggers before the scheduled job is executed
     * @param job current ScheduledExecution
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobExecution(ScheduledExecution job, JobPreExecutionEvent event) {
        def plugins = createConfiguredPlugins(job.project)
        handleEvent(event, EventType.PRE_EXECUTION, plugins)
    }

    /**
     * It triggers before the scheduled job is saved
     * @param job current ScheduledExecution
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobSave(ScheduledExecution job, JobPersistEvent event) {
        def plugins = createConfiguredPlugins(job.project)
        handleEvent(event, EventType.BEFORE_SAVE, plugins)
    }

    /**
     * Load configured ProjectPlugin instances for the job
     * @param project
     * @return
     */
    List<NamedProjectPlugin> createConfiguredPlugins(String project) {
        List<NamedProjectPlugin> configured = []
        def rundeckProject = frameworkService.getFrameworkProject(project)
        def defaultPluginTypes = new HashSet<String>(getProjectDefaultProjectPluginTypes(rundeckProject))

        defaultPluginTypes.each{type->
            def configuredPlugin = pluginService.configurePlugin(
                    type,
                    [:],
                    project,
                    frameworkService.rundeckFramework,
                    ProjectPlugin
            )
            if (!configuredPlugin) {
                //TODO: could not load plugin, or config was invalid
                return
            }
            configured << new NamedProjectPlugin(plugin: (ProjectPlugin) configuredPlugin.instance, name: type)
        }
        configured
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @param plugins list of NamedProjectPlugin
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus handleEvent(def event, EventType eventType, List<NamedProjectPlugin> plugins) {
        if (!plugins) {
            return null
        }
        def errors = [:]
        def results = [:]
        Exception firstErr
        JobEventStatus prevResult = null
        def prevEvent = event
        boolean success = true
        for (NamedProjectPlugin plugin : plugins) {
            try {

                def curEvent = mergeEvent(prevResult, prevEvent)
                JobEventStatus result = handleEventForPlugin(eventType, plugin, curEvent)
                if (result != null && !result.successful) {
                    success = false
                    log.info("Result from plugin is false an exception will be thrown")
                    if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
                        throw new ProjectPluginException(result.getDescription())
                    } else {
                        throw new ProjectPluginException(
                                "Response from $plugin.name is false, but no description was provided by the plugin"
                        )
                    }

                }
                if (result != null && result.useNewValues()) {
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
                log.error("Error (ProjectPlugin:$name/$eventType): $e.message", e)
            }
            if (firstErr) {
                throw firstErr
            }
        }

        mergeEventResult(success, prevResult, prevEvent, !results.isEmpty())

    }

    /**
     * Merge original event, event status result, return a new result
     * @param success overall success
     * @param jobEventStatus result of plugin handling event
     * @param jobEvent event
     * @return result with merged contents for the type of event
     */
    JobEventStatus mergeEventResult(boolean success, final JobEventStatus jobEventStatus, final Object jobEvent, boolean useNewValues) {
        if (jobEvent instanceof JobPreExecutionEventImpl) {
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
    Object mergeEvent(final JobEventStatus jobEventStatus, final Object jobEvent) {
        if (jobEvent instanceof JobPreExecutionEventImpl) {
            HashMap<String, String> newOptionsValues = mergePreExecutionOptionsValues(
                    jobEvent.optionsValues,
                    jobEventStatus
            )
            return new JobPreExecutionEventImpl(
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

    JobEventStatus handleEventForPlugin(
            EventType eventType,
            NamedProjectPlugin plugin,
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
            JobEventStatus jobEventStatus
    ) {
        def newOptionsValues = new HashMap<String, String>(optionsValues ?: [:])
        if (jobEventStatus && jobEventStatus.useNewValues() && jobEventStatus.optionsValues) {
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
    TreeSet<JobOption> mergePersistOptions(SortedSet<JobOption> initial, JobEventStatus jobEventStatus) {
        SortedSet<JobOption> options = initial ? new TreeSet<JobOption>(initial) : null

        if (jobEventStatus && jobEventStatus.useNewValues()) {
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
     * @param provider ProjectPlugin provider name
     * @return true if the job plugin name is enabled for the project
     */
    Set<String> getProjectDefaultProjectPluginTypes(IRundeckProject rundeckProject) {
        rundeckProject.getProjectProperties().findAll {
            it.key.startsWith(CONF_PROJECT_ENABLED) && it.value == 'true'
        }.collect {
            it.key.substring(CONF_PROJECT_ENABLED.length())
        }
    }

    class NamedProjectPlugin implements ProjectPlugin {
        @Delegate ProjectPlugin plugin
        String name
    }

}

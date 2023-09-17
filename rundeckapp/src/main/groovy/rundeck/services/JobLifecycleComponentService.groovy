package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.jobs.JobEvent
import com.dtolabs.rundeck.core.jobs.JobLifecycleComponent
import com.dtolabs.rundeck.core.jobs.JobLifecycleComponentException
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobLifecyclePluginProviderService
import grails.events.annotation.Subscriber
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.core.projects.ProjectConfigurable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution
import rundeck.services.feature.FeatureService

/**
 * Provides capability to execute certain based on an event
 * Created by rnavarro
 * Date: 8/23/19
 * Time: 10:37 AM
 */
@CompileStatic
@Slf4j
class JobLifecycleComponentService implements ProjectConfigurable, ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(JobLifecycleComponentService)

    @Autowired
    PluginService pluginService

    @Autowired
    FrameworkService frameworkService

    @Autowired
    FeatureService featureService

    @Autowired
    JobLifecyclePluginProviderService jobLifecyclePluginProviderService
    public static final String CONF_PROJECT_ENABLED = 'project.enable.jobLifecyclePlugin.'

    Map<String, String> configPropertiesMapping
    Map<String, String> configProperties
    List<Property> projectConfigProperties

    Map<String, JobLifecycleComponent> beanComponents

    ApplicationContext applicationContext

    @Subscriber('rundeck.bootstrap')
    void init() throws Exception {

        beanComponents = applicationContext.getBeansOfType(JobLifecycleComponent)

        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing " + JobLifecycleComponentService.getSimpleName())
            beanComponents?.each {
                LOG.debug("Loaded JobLifecycleComponent Bean: ${it.getKey()}")
            }
        }
    }

    /**
     *
     * It loads the plugin properties to be shown under the project configuration
     */
    @CompileDynamic
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
    JobLifecycleStatus beforeJobExecution(String projectName, JobPreExecutionEvent event)
            throws JobLifecycleComponentException {
        def components = loadProjectComponents(projectName)
        handleEvent(event, EventType.PRE_EXECUTION, components)
    }

    /**
     * It triggers before the scheduled job is saved
     * @param job current ScheduledExecution
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobLifecycleStatus beforeJobSave(String projectName, JobPersistEvent event) throws JobLifecycleComponentException {
        def plugins = loadProjectComponents(projectName)
        handleEvent(event, EventType.BEFORE_SAVE, plugins)
    }

    /**
     * Load all available components for a project.
     * @param project
     * @return
     */
    List<NamedJobLifecycleComponent> loadProjectComponents(String project) {
        List compList = []
        if (beanComponents) {
            compList.addAll(beanComponents.collect { name, component->
                new NamedJobLifecycleComponent(
                    component: component,
                    name: component.class.simpleName)
            })
        }
        compList.addAll(loadProjectConfiguredPlugins(project))
        compList
    }

    /**
     * Load configured JobLifecyclePlugin instances
     * @param project
     * @return
     */
    List<NamedJobLifecycleComponent> loadProjectConfiguredPlugins(String project) {
        List<NamedJobLifecycleComponent> configured = []
        def rundeckProject = frameworkService.getFrameworkProject(project)
        def defaultPluginTypes = new LinkedHashSet<String>(getProjectDefaultJobLifecyclePlugins(rundeckProject))

        defaultPluginTypes.each{type->
            def configuredPlugin = pluginService.configurePlugin(
                    type,
                    [:],
                    project,
                    frameworkService.rundeckFramework,
                    JobLifecyclePlugin
            )
            if (!configuredPlugin) {
                //could not load plugin, or config was invalid
                LOG.warn("Could not configure job lifecycle plugin [${type}] for project [${project}]")
                return
            }
            configured << new NamedJobLifecycleComponent(component: (JobLifecyclePlugin) configuredPlugin.instance, name: type)
        }
        configured
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @param components list of NamedJobLifecycleComponent
     * @return JobEventStatus response from plugin implementation
     */
    JobLifecycleStatus handleEvent(def receivedEvent, EventType eventType, List<NamedJobLifecycleComponent> components)
            throws JobLifecycleComponentException {
        
        if (!components) {
            return null
        }
        
        Map<String, Exception> errors = [:]
        boolean success = true
        boolean useNewOptionValues = false
        boolean useNewExecutionMetadata = false
        
        def currentEvent = receivedEvent
        
        for (NamedJobLifecycleComponent component : components) {
            try {
                JobLifecycleStatus result = handleEventForPlugin(eventType, component, currentEvent)

                if (result != null && !result.successful) {
                    throw new JobLifecycleComponentException("Error dispatcing event [${eventType}] for component [${component.name}]: " + result?.errorMessage)
                }
                
                useNewOptionValues = result?.isUseNewValues() || useNewOptionValues
                useNewExecutionMetadata = result?.isUseNewMetadata() || useNewExecutionMetadata
                currentEvent = mergeEvent(result, currentEvent)
                
            } catch (Exception e) {
                success = false
                errors[component.name] = e
            }
        }
        
        if (!success || errors) {
            LOG.warn("Errors processing Job Component Event [${eventType}]:")
            errors.each { name, e -> 
                LOG.warn(" -- For component [${name}]: " + e.getMessage())
                LOG.debug(" -- For component [${name}]: " + e.getMessage(), e)
            }
            throw new JobLifecycleComponentException("Job could not start due to the following errors: " + errors.collect { name, e ->
                "${e.message}"
            }.join(", "))
        }

        return createEventResult(success, currentEvent, useNewOptionValues, useNewExecutionMetadata)
    }

    /**
     * Create a new event result.
     * @param success overall success
     * @param jobEvent event Processed event.
     * @return result with processed contents for the type of event
     */
    static JobLifecycleStatus createEventResult(boolean success, final Object jobEvent, boolean useNewOptionValues, boolean useNewExecutionMetadata) {

        if (jobEvent instanceof JobPreExecutionEvent) {
            return new JobEventStatusImpl(
                successful: success,
                eventType: jobEvent.getClass(),
                useNewValues: useNewOptionValues,
                optionsValues: jobEvent.optionsValues,
                useNewMetadata: useNewExecutionMetadata,
                newExecutionMetadata: jobEvent.executionMetadata
            )
        } else if (jobEvent instanceof JobPersistEvent) {
            return new JobEventStatusImpl(
                successful: success,
                eventType: jobEvent.getClass(),
                options: jobEvent.options,
                useNewValues: useNewOptionValues
            )
        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }

    /**
     * Merge
     * @param jobEventStatus
     * @param jobEvent
     * @return
     */
    static Object mergeEvent(final JobLifecycleStatus jobEventStatus, final def jobEvent) {

        if (jobEvent instanceof JobPreExecutionEvent) {
            def newEvent = new JobPreExecutionEventImpl(jobEvent);
            newEvent.setOptionsValues(mergePreExecutionOptionsValues(jobEvent.optionsValues, jobEventStatus))
            newEvent.setExecutionMetadata(mergePreExecutionMetadata(jobEvent.executionMetadata, jobEventStatus))
            return newEvent

        } else if (jobEvent instanceof JobPersistEvent) {
            def newEvent = new JobPersistEventImpl(jobEvent)
            newEvent.setOptions(mergePersistOptions(jobEvent.options, jobEventStatus))
            return newEvent

        } else {
            throw new IllegalArgumentException("Unexpected type")
        }
    }

    @CompileDynamic
    static JobLifecycleStatus handleEventForPlugin(
        EventType eventType,
        NamedJobLifecycleComponent plugin,
        Object event
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
    static Map<String, String> mergePreExecutionOptionsValues(
            Map<String, String> optionsValues,
            JobLifecycleStatus jobEventStatus
    ) {
        if (jobEventStatus?.isUseNewValues() && jobEventStatus.optionsValues) {
            def newOptionsValues = new HashMap<String, String>(optionsValues ?: [:])
            newOptionsValues.putAll(jobEventStatus.optionsValues)
            return newOptionsValues
        }
        return optionsValues
    }
  
    /**
     * Merge optionsValues map from event result if useNewValues is true and optionsValues is set
     * @param optionsValues original optionsValues map, or null
     * @param jobEventStatus result of pre execution event
     * @return new map with merged optionsvalues
     */
    static Map mergePreExecutionMetadata(
        Map executionMetadata,
        JobLifecycleStatus jobEventStatus
    ) {
        if (jobEventStatus?.isUseNewMetadata() && jobEventStatus.newExecutionMetadata) {
            def newExecutionMetadata = new HashMap(executionMetadata ?: [:])
            newExecutionMetadata.putAll(jobEventStatus.newExecutionMetadata)
            return newExecutionMetadata
        }
        return executionMetadata
    }

    /**
     * It replaces initial JobOption set with result of event if useNewValues is specified, or return null if
     * the initial set was null and result value was null or useNewValues was not set
     * @param initial initial set, or null
     * @param jobEventStatus result of event
     * @return merged set, or null
     */
    @CompileDynamic
    static TreeSet<JobOption> mergePersistOptions(SortedSet<JobOption> initial, JobLifecycleStatus jobEventStatus) {
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
        }.toSet()
    }

    enum EventType {
        PRE_EXECUTION("preExecution"),
        BEFORE_SAVE("beforeSave")

        private final String value

        EventType(String value) {
            this.value = value
        }

        String getValue() {
            this.value
        }
    }
    
}

@CompileStatic
class NamedJobLifecycleComponent implements JobLifecycleComponent {
    @Delegate JobLifecycleComponent component
    String name
    
    boolean isPlugin() {
        return component instanceof JobLifecyclePlugin
    }

    JobLifecycleStatus beforeJobExecution(JobPreExecutionEvent event) throws JobLifecycleComponentException {
        return component.beforeJobExecution(event)
    }

    JobLifecycleStatus beforeSaveJob(JobPersistEvent event) throws JobLifecycleComponentException {
        return component.beforeSaveJob(event)
    }
}

@CompileStatic
class JobEventStatusImpl implements JobLifecycleStatus {
    boolean successful
    Map optionsValues
    boolean useNewValues
    SortedSet<JobOption> options
    boolean useNewMetadata
    Map newExecutionMetadata
    String errorMessage

    Class<? extends JobEvent> eventType

    @Override
    Class<? extends JobEvent> getJobEventType() {
        return this.eventType
    }


    @Override
    public String getErrorMessage() {
        return this.errorMessage
    }

}

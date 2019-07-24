package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.IJobPluginService
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.jobs.JobPersistEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPlugin
import com.dtolabs.rundeck.plugins.jobs.JobPreExecutionEventImpl

import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobPluginProviderService
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

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

    LinkedHashMap<String, String> configPropertiesMapping
    LinkedHashMap<String, String> configProperties
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
        List<Property> projectConfigProperties = new ArrayList<Property>()
        LinkedHashMap<String, String> configPropertiesMapping = []
        LinkedHashMap<String, String> configProperties = []
        listJobPlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        booleanType 'jobPlugin' + name
                        title 'Enable ' + name
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
    JobEventStatus beforeJobExecution(JobPreExecutionEvent event){
        executeLifeCycle(event, EventType.PRE_EXECUTION)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobStarts(JobExecutionEvent event){
        executeLifeCycle(event, EventType.BEFORE_RUN)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus afterJobEnds(JobExecutionEvent event){
        executeLifeCycle(event, EventType.AFTER_RUN)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobSave(JobPersistEvent event){
        executeLifeCycle(event, EventType.BEFORE_SAVE)
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus executeLifeCycle(def event, def eventType){
        JobEventStatus result
        if(!featureService?.featurePresent('job-plugin', false)){
            return result
        }
        def instanceMap = [:]
        IRundeckProject rundeckProject = frameworkService?.getFrameworkProject(event.getProjectName())
        def jlcps = listJobPlugins()
        jlcps.each { String name, DescribedPlugin describedPlugin ->
            if (rundeckProject?.hasProperty(CONF_PROJECT_ENABLE_JOB + name) &&
                    rundeckProject?.getProperty(CONF_PROJECT_ENABLE_JOB + name).equals("true")) {
                try {
                    JobPlugin plugin = (JobPlugin) describedPlugin.instance
                    if(eventType == EventType.BEFORE_RUN){
                        result = plugin.beforeJobStarts(event)
                    }else if(eventType == EventType.AFTER_RUN) {
                        result = plugin.afterJobEnds(event)
                    }else if(eventType == EventType.PRE_EXECUTION) {
                        def newEventInstance = new JobPreExecutionEventImpl(event)
                        result = plugin.beforeJobExecution(newEventInstance)
                        if(result?.useNewValues() == true){
                            instanceMap.put(name, result)
                        }
                    }else if(eventType == EventType.BEFORE_SAVE){
                        def newEventInstance = new JobPersistEventImpl(event)
                        result = plugin.beforeSaveJob(newEventInstance)
                        if(result?.useNewValues() == true){
                            instanceMap.put(name, result)
                        }
                    }
                    if (result != null && !result.isSuccessful()) {
                        log.info("Result from plugin is false an exception will be thrown")
                        if (result.getDescription() != null && !result.getDescription().trim().isEmpty()) {
                            throw new JobPluginException(result.getDescription())
                        } else {
                            throw new JobPluginException('Response from ' + name + ' is false, but no description was provided by the plugin')
                        }
                    }
                } catch (JobPluginException e) {
                    throw e
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                    throw e
                }
            }
        }
        mergeResult(instanceMap, eventType, result)
        result
    }

    /**
     * It merges the original event with the new ones from the plugins
     * @param event original job event
     * @param instanceMap a map containing the instances sent to the plugins (mapped by plugin name)
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
}

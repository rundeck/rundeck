package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.jobs.JobEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.jobs.JobPlugin
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

public class JobPluginService implements ApplicationContextAware, ProjectConfigurable {

    ApplicationContext applicationContext
    PluginService pluginService
    JobPluginProviderService jobPluginProviderService
    FrameworkService frameworkService
    public static final String CONF_PROJECT_ENABLE_JOB = 'project.enable.jobPlugin.'

    LinkedHashMap<String, String> configPropertiesMapping
    LinkedHashMap<String, String> configProperties
    List<Property> projectConfigProperties

    enum EventType{
        BEFORE('beforeJob'), AFTER('afterJob')
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
        return pluginService.listPlugins(JobPlugin, jobPluginProviderService)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus beforeJobStarts(JobEvent event){
        executeLifeCycle(event, EventType.BEFORE)
    }

    /**
     *
     * @param event job event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus afterJobEnds(JobEvent event){
        executeLifeCycle(event, EventType.AFTER)
    }

    /**
     *
     * @param event job event
     * @param eventType type of event
     * @return JobEventStatus response from plugin implementation
     */
    JobEventStatus executeLifeCycle(def event, def eventType){
        JobEventStatus result = new JobEventStatus()
        String projectName = event.getExecutionContext()?.getFrameworkProject()
        IRundeckProject rundeckProject = event.getExecutionContext()?.getFramework()?.getProjectManager()?.getFrameworkProject(projectName)
        def jlcps = listJobPlugins()
        jlcps.each { String name, DescribedPlugin describedPlugin ->
            if (rundeckProject?.hasProperty(CONF_PROJECT_ENABLE_JOB + name) &&
                    rundeckProject?.getProperty(CONF_PROJECT_ENABLE_JOB + name).equals("true")) {
                try {
                    JobPlugin plugin = (JobPlugin) describedPlugin.instance
                    if(eventType.equals(EventType.BEFORE)){
                        result = plugin.beforeJobStarts(event)
                    }else if(eventType.equals(EventType.AFTER)) {
                        result = plugin.afterJobEnds(event)
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
        result
    }

}

package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus
import com.dtolabs.rundeck.core.logging.LoggingManager
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.jobs.JobLifeCyclePlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.services.JobLifeCyclePluginProviderService
import org.rundeck.core.projects.ProjectConfigurable
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Provides capability to execute certain task based on job life cycle
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */

public class JobLifeCyclePluginService implements ApplicationContextAware, ProjectConfigurable {

    ApplicationContext applicationContext
    PluginService pluginService
    JobLifeCyclePluginProviderService jobLifeCyclePluginProviderService
    FrameworkService frameworkService
    public static final String CONF_PROJECT_ENABLE_JOB_ON_BEFORE = 'project.enable.jobLifeCycle.on.before.'

    LinkedHashMap<String, String> configPropertiesMapping
    LinkedHashMap<String, String> configProperties
    List<Property> projectConfigProperties

    def loadProperties(){
        List<Property> projectConfigProperties = new ArrayList<Property>()
        LinkedHashMap<String, String> configPropertiesMapping = []
        LinkedHashMap<String, String> configProperties = []
        listJobLifeCyclePlugins().each { String name, DescribedPlugin describedPlugin ->
            projectConfigProperties.add(
                    PropertyBuilder.builder().with {
                        booleanType 'jobLifeCycle'+ name
                        title 'Enable '+ name + ' before job starts'
                        description describedPlugin.getDescription().getDescription()
                        required(false)
                        defaultValue null
                        renderingOption('booleanTrueDisplayValueClass', 'text-warning')
                    }.build()
            )
            configPropertiesMapping.put('jobLifeCycle' + name, CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name)
            configProperties.put('jobLifeCycle' + name, 'jobLifeCycle')
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

    ValidatedPlugin validatePluginConfig(String project, String name, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolver(project, config), PropertyScope.Instance, PropertyScope.Project)
    }
    ValidatedPlugin validatePluginConfig(String name, Map projectProps, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolverWithProps(projectProps, config), PropertyScope.Instance, PropertyScope.Project)
    }

    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    DescribedPlugin getJobLifeCycleDescriptor(String name) {
        return pluginService.getPluginDescriptor(name, jobLifeCyclePluginProviderService)
    }

    Map listJobLifeCyclePlugins(){
        return pluginService.listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService)
    }

    JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event){

        JobLifeCycleStatus result = new JobLifeCycleStatus()
        String projectName = event.getExecutionContext().getFrameworkProject()
        IRundeckProject rundeckProject = event.getExecutionContext().getFramework().getProjectManager().getFrameworkProject(projectName)
        def jlcps = listJobLifeCyclePlugins()
        jlcps.each{ String name, DescribedPlugin describedPlugin ->
            if (rundeckProject.hasProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) &&
                    rundeckProject.getProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) == 'true') {
                try{
                    JobLifeCyclePlugin plugin = (JobLifeCyclePlugin) describedPlugin.instance
                    result = plugin.beforeJobStarts(event)
                    if(!result.isSuccessful()){
                        log.info("Result from plugin is false an exception will be thrown")
                        if(result.getDescription() != null && !result.getDescription().trim().isEmpty()){
                            throw new JobLifeCycleException (result.getDescription())
                        }else{
                            throw new JobLifeCycleException ('Response from ' + name + ' is false, but no description was provided by the plugin')
                        }
                    }
                }catch(JobLifeCycleException e){
                    throw e
                }catch (Exception e){
                    log.error(e.getMessage(), e)
                    throw e
                }
            }
        }
        result
    }

    JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event){
        JobLifeCycleStatus result = new JobLifeCycleStatus()
        String projectName = event.getExecutionContext().getFrameworkProject()
        IRundeckProject rundeckProject = event.getExecutionContext().getFramework().getProjectManager().getFrameworkProject(projectName)
        def jlcps = listJobLifeCyclePlugins()
        jlcps.each{ String name, DescribedPlugin describedPlugin ->
            if (rundeckProject.hasProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) &&
                    rundeckProject.getProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) == 'true') {
                try{
                    JobLifeCyclePlugin plugin = (JobLifeCyclePlugin) describedPlugin.instance
                    result = plugin.afterJobEnds(event)
                    if(!result.isSuccessful()){
                        log.info("Result from plugin is false an exception will be thrown")
                        if(result.getDescription() != null && !result.getDescription().trim().isEmpty()){
                            throw new JobLifeCycleException (result.getDescription())
                        }else{
                            throw new JobLifeCycleException ('Response from ' + name + ' is false, but no description was provided by the plugin')
                        }
                    }
                }catch(JobLifeCycleException e){
                    throw e
                }catch (Exception e){
                    log.error(e.getMessage(), e)
                    throw e
                }
            }
        }
        result
    }

}

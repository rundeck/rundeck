package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
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
    def pluginService
    def JobLifeCyclePluginProviderService jobLifeCyclePluginProviderService
    def FrameworkService frameworkService
    public static final String CONF_PROJECT_ENABLE_JOB_ON_BEFORE = 'project.enable.jobLifeCycle.on.before.'

    def LinkedHashMap<String, String> configPropertiesMapping = []
    def LinkedHashMap<String, String> configProperties = []

    @Override
    Map<String, String> getCategories() { configProperties }

    @Override
    Map<String, String> getPropertiesMapping() { configPropertiesMapping }

    @Override
    List<Property> getProjectConfigProperties() {
        List<Property> properties = new ArrayList<Property>()
        listJobLifeCyclePlugins().each { String name, DescribedPlugin describedPlugin ->
            properties.add(
                PropertyBuilder.builder().with {
                    booleanType 'enableExecution'+ name
                    title 'Enable '+ name + ' before job starts'
                    description 'Description from the plugin jlcp'
                    required(false)
                    defaultValue null
                    renderingOption('booleanTrueDisplayValueClass', 'text-warning')
                }.build()
            )
            configPropertiesMapping.put('enableExecution' + name, CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name)
            configProperties.put('enableExecution' + name, 'executionMode')
        }
        properties
    }

    def ValidatedPlugin validatePluginConfig(String project, String name, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolver(project, config), PropertyScope.Instance, PropertyScope.Project)
    }
    def ValidatedPlugin validatePluginConfig(String name, Map projectProps, Map config) {
        return pluginService.validatePlugin(name, jobLifeCyclePluginProviderService,
                frameworkService.getFrameworkPropertyResolverWithProps(projectProps, config), PropertyScope.Instance, PropertyScope.Project)
    }

    /**
     *
     * @param name
     * @return map containing [instance:(plugin instance), description: (map or Description), ]
     */
    def DescribedPlugin getJobLifeCycleDescriptor(String name) {
        return pluginService.getPluginDescriptor(name, jobLifeCyclePluginProviderService)
    }

    def Map listJobLifeCyclePlugins(){
        return pluginService.listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService)
    }

    //TODO: change exception message (and probably exception type
    //needs some talk about what to do if one or many of the calls are false
    def boolean onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                                 LoggingManager workflowLogManager){

        String projectName = executionContext.getFrameworkProject()
        IRundeckProject rundeckProject = executionContext.getFramework().getProjectManager().getFrameworkProject(projectName)
        def jlcps = listJobLifeCyclePlugins()
        jlcps.each{ String name, DescribedPlugin describedPlugin ->
            if (rundeckProject.hasProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) &&
                    rundeckProject.getProperty(CONF_PROJECT_ENABLE_JOB_ON_BEFORE + name) == 'true') {
                JobLifeCyclePlugin plugin = (JobLifeCyclePlugin) describedPlugin.instance
                boolean result = plugin.onBeforeJobStart(item, executionContext, workflowLogManager)
                if(!result){
                    throw new Exception ("Job not allowed to be executed")
                }
            }
        }
        return true;
    }

}

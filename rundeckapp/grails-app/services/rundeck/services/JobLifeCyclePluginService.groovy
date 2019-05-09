package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.logging.LoggingManager
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.jobs.JobLifeCyclePlugin
import com.dtolabs.rundeck.server.plugins.services.JobLifeCyclePluginProviderService
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Provides capability to execute certain task based on job life cycle
 * Created by rnavarro
 * Date: 5/07/19
 * Time: 10:32 AM
 */

public class JobLifeCyclePluginService implements ApplicationContextAware {

    ApplicationContext applicationContext
    def pluginService
    def JobLifeCyclePluginProviderService jobLifeCyclePluginProviderService
    def FrameworkService frameworkService

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
    //check for project configuration in order to know whether to run the plugin or not
    //needs some talk about what to do if one or many of the calls are false
    def boolean onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                                 LoggingManager workflowLogManager){
        def jlcps = listJobLifeCyclePlugins()
        jlcps.each{ String name, DescribedPlugin describedPlugin ->
            JobLifeCyclePlugin plugin = (JobLifeCyclePlugin) describedPlugin.instance
            boolean result = plugin.onBeforeJobStart(item, executionContext, workflowLogManager)
            if(!result){
                throw new Exception ("Job not allowed to be executed")
            }
        }
        return true;
    }

}

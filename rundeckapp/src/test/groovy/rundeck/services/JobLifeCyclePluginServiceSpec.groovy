package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus
import com.dtolabs.rundeck.core.logging.LoggingManager
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.plugins.jobs.JobLifeCyclePlugin
import com.dtolabs.rundeck.server.plugins.services.JobLifeCyclePluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import spock.lang.Specification

@TestFor(JobLifeCyclePluginService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class JobLifeCyclePluginServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.jobLifeCycle.on.before.TestPlugin") >> true
        getProperty("project.enable.jobLifeCycle.on.before.TestPlugin") >> 'true'
    }
    def projectManager = Mock(ProjectManager) {
        getFrameworkProject("Test") >> iRundeckProject
    }
    def framework = Mock(Framework){
        getProjectManager() >> projectManager

    }
    StepExecutionContext executionContext = Mock(StepExecutionContext) {
        getFrameworkProject() >> "Test"
        getFramework() >> framework
    }
    def jobLifeCyclePluginProviderService = Mock(JobLifeCyclePluginProviderService)


    static class JobLifeCyclePluginImpl implements JobLifeCyclePlugin {

        @Override
        public JobLifeCycleStatus onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                                                   LoggingManager workflowLogManager)throws JobLifeCycleException{
            throw new JobLifeCycleException("Test job life cycle exception")
        }
    }

    def "custom plugin successful answer"() {
        given:
        service.jobLifeCyclePluginProviderService = jobLifeCyclePluginProviderService
        def plugin = Mock(JobLifeCyclePluginImpl){
            onBeforeJobStart(item, executionContext, null) >> Mock(JobLifeCycleStatus){
                isSuccessful() >> true
            }
        }
        def describedPlugin = new DescribedPlugin(plugin, null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobLifeCycleStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        result.isSuccessful()
    }

    def "custom plugin exception thrown for returning false"() {
        given:
        service.jobLifeCyclePluginProviderService = jobLifeCyclePluginProviderService
        def plugin = Mock(JobLifeCyclePluginImpl){
            onBeforeJobStart(item, executionContext, null) >> Mock(JobLifeCycleStatus){
                isSuccessful() >> false
            }
        }
        def describedPlugin = new DescribedPlugin(plugin, null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobLifeCycleStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        thrown(JobLifeCycleException)
    }

    def "custom plugin exception thrown from the plugin"() {
        given:
        service.jobLifeCyclePluginProviderService = jobLifeCyclePluginProviderService
        def describedPlugin = new DescribedPlugin(new JobLifeCyclePluginImpl(), null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(JobLifeCyclePlugin, jobLifeCyclePluginProviderService) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobLifeCycleStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        thrown(JobLifeCycleException)
    }

}

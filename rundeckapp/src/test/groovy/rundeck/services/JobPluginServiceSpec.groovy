package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.plugins.jobs.JobEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPlugin
import com.dtolabs.rundeck.server.plugins.services.JobPluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import spock.lang.Specification

@TestFor(JobPluginService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class JobPluginServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.jobPlugin.TestPlugin") >> true
        getProperty("project.enable.jobPlugin.TestPlugin") >> 'true'
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
    def jobPluginProviderService = Mock(JobPluginProviderService)


    static class JobPluginImpl implements JobPlugin {

        @Override
        public JobEventStatus beforeJobStarts(JobEvent event)throws JobPluginException{
            throw new JobPluginException("Test job life cycle exception")
        }

        @Override
        public JobEventStatus afterJobEnds(JobEvent event)throws JobPluginException{
            throw new JobPluginException("Test job life cycle exception")
        }
    }

    def "custom plugin successful answer"() {
        given:
        service.jobPluginProviderService = jobPluginProviderService
        def plugin = Mock(JobPluginImpl){
            beforeJobStarts(_) >> Mock(JobEventStatus){
                isSuccessful() >> true
            }
        }
        def describedPlugin = new DescribedPlugin(plugin, null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(_,_) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        result.isSuccessful()
    }

    def "custom plugin exception thrown for returning false"() {
        given:
        service.jobPluginProviderService = jobPluginProviderService
        def plugin = Mock(JobPluginImpl){
            beforeJobStarts(_) >> Mock(JobEventStatus){
                isSuccessful() >> false
            }
        }
        def describedPlugin = new DescribedPlugin(plugin, null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(JobPlugin, jobPluginProviderService) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        thrown(JobPluginException)
    }

    def "custom plugin exception thrown from the plugin"() {
        given:
        service.jobPluginProviderService = jobPluginProviderService
        def describedPlugin = new DescribedPlugin(new JobPluginImpl(), null, 'TestPlugin')
        service.pluginService = Mock(PluginService){
            listPlugins(JobPlugin, jobPluginProviderService) >> ["TestPlugin":describedPlugin]
        }
        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        thrown(JobPluginException)
    }

}

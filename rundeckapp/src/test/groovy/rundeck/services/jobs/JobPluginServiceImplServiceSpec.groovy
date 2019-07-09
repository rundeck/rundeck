package rundeck.services.jobs

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
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
import rundeck.services.JobPluginService
import spock.lang.Specification

@TestFor(JobPluginServiceImplService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class JobPluginServiceImplServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.jobPlugin.before.TestInnerPlugin") >> true
        getProperty("project.enable.jobPlugin.before.TestInnerPlugin") >> 'true'
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

    JobPluginProviderService innerJobPluginProviderService = Mock(JobPluginProviderService)

    static class JobPluginImpl implements JobPlugin {

        @Override
        public JobEventStatus beforeJobStarts(JobEvent event)throws JobPluginException{
            return new JobEventStatus(true, "Example description");
        }

        @Override
        public JobEventStatus afterJobEnds(JobEvent event)throws JobPluginException{
            return new JobEventStatus(true, "Example description");
        }
    }

    def "custom plugin successful answer"() {
        given:
        service.jobPluginService = Mock(JobPluginService){
            beforeJobStarts(_) >> Mock(JobEventStatus){
                isSuccessful() >> true
            }
        }

        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        result.isSuccessful()
    }

    def "exception thrown"() {
        given:
        service.jobPluginService = Mock(JobPluginService){
            beforeJobStarts(_) >> {
                throw new JobPluginException("Test exception")
            }
        }

        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        thrown(JobPluginException)
    }

    def "returns false with message"() {
        given:
        service.jobPluginService = Mock(JobPluginService){
            beforeJobStarts(_) >> Mock(JobEventStatus){
                isSuccessful() >> false
                getDescription() >> "It has description because it is also false"
            }
        }

        when:
        JobEventStatus result = service.beforeJobStarts(new JobEventImpl(executionContext))
        then:
        !result.isSuccessful()
        result.getDescription() == "It has description because it is also false"
    }

}

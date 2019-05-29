package rundeck.services.jobs

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus
import com.dtolabs.rundeck.plugins.jobs.JobLifeCycleEventImpl
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
import rundeck.services.JobLifeCyclePluginService
import spock.lang.Specification

@TestFor(JobLifeCycleServiceImplService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class JobLifeCycleServiceImplServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.jobLifeCycle.on.before.TestInnerPlugin") >> true
        getProperty("project.enable.jobLifeCycle.on.before.TestInnerPlugin") >> 'true'
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

    JobLifeCyclePluginProviderService innerJobLifeCyclePluginProviderService = Mock(JobLifeCyclePluginProviderService)

    static class JobLifeCyclePluginImpl implements JobLifeCyclePlugin {

        @Override
        public JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event)throws JobLifeCycleException{
            return new JobLifeCycleStatus(true, "Example description");
        }

        @Override
        public JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event)throws JobLifeCycleException{
            return new JobLifeCycleStatus(true, "Example description");
        }
    }

    def "custom plugin successful answer"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            beforeJobStarts(_) >> Mock(JobLifeCycleStatus){
                isSuccessful() >> true
            }
        }

        when:
        JobLifeCycleStatus result = service.beforeJobStarts(new JobLifeCycleEventImpl(executionContext))
        then:
        result.isSuccessful()
    }

    def "exception thrown"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            beforeJobStarts(_) >> {
                throw new JobLifeCycleException("Test exception")
            }
        }

        when:
        JobLifeCycleStatus result = service.beforeJobStarts(new JobLifeCycleEventImpl(executionContext))
        then:
        thrown(JobLifeCycleException)
    }

    def "returns false with message"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            beforeJobStarts(_) >> Mock(JobLifeCycleStatus){
                isSuccessful() >> false
                getDescription() >> "It has description because it is also false"
            }
        }

        when:
        JobLifeCycleStatus result = service.beforeJobStarts(new JobLifeCycleEventImpl(executionContext))
        then:
        !result.isSuccessful()
        result.getDescription() == "It has description because it is also false"
    }

}

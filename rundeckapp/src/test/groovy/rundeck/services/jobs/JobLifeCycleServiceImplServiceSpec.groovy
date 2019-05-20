package rundeck.services.jobs

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobStatus
import com.dtolabs.rundeck.core.logging.LoggingManager
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.plugins.jobs.JobLifeCyclePlugin
import com.dtolabs.rundeck.server.plugins.services.JobLifeCyclePluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.JobLifeCyclePluginService
import rundeck.services.PluginService
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
        public JobStatus onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                                          LoggingManager workflowLogManager)throws JobLifeCycleException{
            return new JobLifeCycleStatus(true, "Example description");
        }
    }

    def "custom plugin successful answer"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            onBeforeJobStart(_,_,_) >> Mock(JobStatus){
                isSuccessful() >> true
            }
        }

        when:
        JobStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        result.isSuccessful()
    }

    def "exception thrown"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            onBeforeJobStart(_,_,_) >> {
                throw new JobLifeCycleException("Test exception")
            }
        }

        when:
        JobStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        thrown(JobLifeCycleException)
    }

    def "returns false with message"() {
        given:
        service.jobLifeCyclePluginService = Mock(JobLifeCyclePluginService){
            onBeforeJobStart(_,_,_) >> Mock(JobStatus){
                isSuccessful() >> false
                getDescription() >> "It has description because it is also false"
            }
        }

        when:
        JobStatus result = service.onBeforeJobStart(item, executionContext, null)
        then:
        !result.isSuccessful()
        result.getDescription() == "It has description because it is also false"
    }

}

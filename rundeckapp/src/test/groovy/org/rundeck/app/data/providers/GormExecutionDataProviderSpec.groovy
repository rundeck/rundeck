package org.rundeck.app.data.providers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Execution
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.execution.RdExecution
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import rundeck.data.paging.RdPageable
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import spock.lang.Specification
import testhelper.TestDomainFactory

import javax.servlet.http.HttpSession

class GormExecutionDataProviderSpec extends Specification implements DataTest {
    GormExecutionDataProvider provider = new GormExecutionDataProvider()

    def setupSpec() {
        mockDomains(Execution, Workflow, CommandExec, PluginStep, JobExec)
    }
    def "Get"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid)

        when:
        def actual = provider.get(e.id)

        then:
        actual.uuid == uuid
    }

    def "GetByUuid"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Execution e = TestDomainFactory.createExecution(uuid: uuid)

        when:
        def actual = provider.getByUuid(uuid)

        then:
        actual.uuid == uuid
        actual.status == e.status
    }

    def "Save"() {
        given:
        String uuid = UUID.randomUUID().toString()
        Date now = new Date()
        RdExecution rdex = new RdExecution()
        rdex.uuid = uuid
        rdex.project = 'test'
        rdex.user = 'tester'
        rdex.dateStarted = now
        rdex.status = 'running'
        rdex.workflow = new RdWorkflow(steps: [new RdWorkflowStep(pluginType: WorkflowStepConstants.TYPE_COMMAND, configuration: [exec:"echo hello"])])

        when:
        def actual = provider.save(rdex)

        then:
        actual.uuid == uuid
        actual.dateStarted == now
        actual.workflow.steps[0].configuration.exec == "echo hello"
    }

    def "Delete"() {
        given:
        provider.metaClass.getSession = { Mock(HttpSession) }
        provider.frameworkService = Mock(FrameworkService) {
            1 * userAuthContext(_) >> Mock(UserAndRolesAuthContext)
        }
        provider.executionService = Mock(ExecutionService) {
            1 * deleteExecution(_,_,_) >> [success: true]
        }
        Execution e = TestDomainFactory.createExecution()

        when:
        def result = provider.delete(e.uuid)

        then:
        result.id == e.uuid
        result.dataType == "Execution"
    }

    def "FindAllExecutionsByJob"() {
        given:
        String jobUuid = UUID.randomUUID().toString()
        String uuid1 = UUID.randomUUID().toString()
        String uuid2 = UUID.randomUUID().toString()
        def e1 = TestDomainFactory.createExecution(uuid: uuid1, jobUuid: jobUuid)
        def e2 = TestDomainFactory.createExecution(uuid: uuid2, jobUuid: jobUuid)

        when:
        def results = provider.findAllExecutionsByJob(jobUuid, new RdPageable())

        then:
        results.size() == 2
        results.find {it.uuid == uuid1 }
        results.find {it.uuid == uuid2 }
    }

}

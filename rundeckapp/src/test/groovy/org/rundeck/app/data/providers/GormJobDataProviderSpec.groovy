package org.rundeck.app.data.providers

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.testing.gorm.DataTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.job.RdJob
import rundeck.data.job.RdLogConfig
import rundeck.data.job.RdNodeConfig
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.data.ScheduledExecutionDataService
import spock.lang.Specification

import javax.security.auth.Subject
import javax.servlet.http.HttpSession

class GormJobDataProviderSpec extends Specification implements DataTest {
    GormJobDataProvider provider = new GormJobDataProvider()

    def setup() {
        def httpSession = Mock(HttpSession) {
            getSubject() >> new Subject()
            getAttribute("user") >> "user"
        }
        provider.metaClass.getSession = {
            httpSession
        }
        mockDomains(ScheduledExecution, Workflow, WorkflowStep, CommandExec)
        mockDataService(ScheduledExecutionDataService)
        provider.scheduledExecutionDataService = applicationContext.getBean(ScheduledExecutionDataService)
    }

    def "Save"() {
        given:
        RdJob rdJob = mockedJob()

        when:
        def saved = provider.save(rdJob)

        then:
        saved.uuid

    }

    def "Delete"() {
        given:
        provider.frameworkService = Mock(FrameworkService) {
            1 * userAuthContext(_) >> Mock(UserAndRolesAuthContext)
        }
        provider.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * deleteScheduledExecution(_,_,_,_) >> new ScheduledExecutionService.DeleteJobResult(success: true)
        }

        when:
        def result = provider._deleteJob(new ScheduledExecution())

        then:
        result.success

    }


    def mockedJob() {
        return Mock(RdJob) {
            getJobName() >> "job1"
            getProject() >> "one"
            getScheduled() >> false
            getNodeConfig() >> new RdNodeConfig()
            getLogConfig() >> new RdLogConfig()
            getComponents() >> [:]
            getWorkflow() >> new RdWorkflow(steps: [new RdWorkflowStep(nodeStep: false,pluginType: "builtin-command",configuration: [exec:"echo hello"])])
        }
    }


}

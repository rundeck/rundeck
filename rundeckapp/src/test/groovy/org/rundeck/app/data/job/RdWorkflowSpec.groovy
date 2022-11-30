package org.rundeck.app.data.job

import grails.util.Holders
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import rundeck.services.FrameworkService
import spock.lang.Specification

class RdWorkflowSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {
        { ->
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
        }
    }

    def setup() {
        Holders.setGrailsApplication(grailsApplication)
    }

    def "invalid workflow with no workflow steps"() {
        when:
        RdWorkflow workflow = new RdWorkflow()

        workflow.validate()

        then:
        workflow.errors.errorCount == 1
        workflow.errors.fieldErrors[0].field == "steps"
        workflow.errors.fieldErrors[0].code == "scheduledExecution.workflow.empty.message"

    }
}

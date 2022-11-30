package org.rundeck.app.data.validation.validators.workflow

import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Description
import grails.config.Settings
import grails.core.GrailsApplication
import grails.util.Holders
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import org.rundeck.app.WorkflowStepConstants
import org.rundeck.app.data.job.RdWorkflow
import org.rundeck.app.data.job.RdWorkflowStep
import org.springframework.context.ApplicationContext
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.PluginService
import spock.lang.Specification

class WorkflowGrailsValidatorSpec extends Specification implements GrailsUnitTest {
    Closure doWithSpring() {
        { ->
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
        }
    }

    def setup() {
        Holders.setGrailsApplication(grailsApplication)
    }

    def "Test Validator with step and log filter plugin errors"() {
        given:
        def fwkSvc = applicationContext.getBean(FrameworkService)
        fwkSvc.validateDescription(_,_,_,_,_,_) >> [valid: false, report: new com.dtolabs.rundeck.core.plugins.configuration.Validator.Report()]
        fwkSvc.pluginService >> Mock(PluginService) {
            getPluginDescriptor(_,_) >> Mock(DescribedPlugin) {
                getDescription() >> Mock(Description)
            }
        }

        when:
        RdWorkflow workflow = new RdWorkflow(threadcount: 1, keepgoing: false, strategy: "node-first")
        workflow.steps = [new RdWorkflowStep(pluginType: WorkflowStepConstants.TYPE_COMMAND,
                nodeStep: true,
                configuration: ["exec": "echo hello","script":"echo hello"],
                pluginConfig: ["LogFilter":[["type":"quiet-output"]]]
        )]

        WorkflowGrailsValidator.validator.call(workflow.steps, workflow, workflow.errors)
        def fieldError = workflow.errors.fieldErrors[0]
        def globalError = workflow.errors.globalErrors[0]

        then:
        workflow.errors.fieldErrorCount == 1
        workflow.errors.globalErrorCount == 1
        fieldError.field == "steps[0].configuration"
        fieldError.code == "scheduledExecution.adhocString.duplicate.message"
        globalError.code == "plugin.configuration.invalid"

    }

}

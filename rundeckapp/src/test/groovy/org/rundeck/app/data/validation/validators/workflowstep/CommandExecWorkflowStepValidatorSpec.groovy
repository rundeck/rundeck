package org.rundeck.app.data.validation.validators.workflowstep

import org.rundeck.app.WorkflowStepConstants
import org.rundeck.app.data.job.RdWorkflowStep
import spock.lang.Specification

class CommandExecWorkflowStepValidatorSpec extends Specification {
    def "Validation tests"() {
        when:
        CommandExecWorkflowStepValidator validator = new CommandExecWorkflowStepValidator()
        RdWorkflowStep step = new RdWorkflowStep(params)
        validator.validate(step, step.errors)

        then:
        step.errors.fieldErrors[0].code == expectedCode

        where:
        expectedCode                                 | params
        WorkflowStepConstants.ERR_CODE_BLANK_COMMAND | [pluginType: WorkflowStepConstants.TYPE_COMMAND, configuration:[script:"echo test"]]
        WorkflowStepConstants.ERR_CODE_BLANK_SCRIPT  | [pluginType: WorkflowStepConstants.TYPE_SCRIPT, configuration:[exec:"echo test"]]
        WorkflowStepConstants.ERR_CODE_BLANK_FILE    | [pluginType: WorkflowStepConstants.TYPE_SCRIPT_FILE, configuration:[exec:"echo test"]]
        WorkflowStepConstants.ERR_CODE_BLANK_URL     | [pluginType: WorkflowStepConstants.TYPE_SCRIPT_URL, configuration:[:]]
        WorkflowStepConstants.ERR_CODE_DUPLICATE_PARAM  | [pluginType: WorkflowStepConstants.TYPE_COMMAND, configuration:[exec:"echo test",script:"echo me"]]
    }
}

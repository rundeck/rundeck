package rundeck.data.validation.validators.workflowstep

import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.data.constants.WorkflowStepConstants

class CommandExecWorkflowStepValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        return WorkflowStepData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        WorkflowStepData wfstep = (WorkflowStepData)target
        def config = wfstep.configuration
        if (!config.exec && WorkflowStepConstants.TYPE_COMMAND == wfstep.pluginType) {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_BLANK_COMMAND)
        } else if (!config.script && WorkflowStepConstants.TYPE_SCRIPT == wfstep.pluginType) {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_BLANK_SCRIPT)
        } else if (!config.scriptfile && WorkflowStepConstants.TYPE_SCRIPT_FILE == wfstep.pluginType) {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_BLANK_FILE)
        } else if (!config.exec && !config.script && !config.scriptfile) {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_BLANK_URL)
        } else {
            def x = ['script', 'exec', 'scriptfile'].grep {
                config[it]
            }
            if (x && x.size() > 1) {
                errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_DUPLICATE_PARAM)
            }
        }
    }
}

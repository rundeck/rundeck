package org.rundeck.app.data.validation.validators.workflowstep

import org.rundeck.app.WorkflowStepConstants
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class JobExecWorkflowStepValidator implements Validator {

    JobDataProvider jobDataProvider
    List<String> authProjects
    boolean strict

    JobExecWorkflowStepValidator(JobDataProvider jobDataProvider, List<String> authProjects, boolean strict) {
        this.jobDataProvider = jobDataProvider
        this.authProjects = authProjects
        this.strict = strict
    }

    @Override
    boolean supports(Class<?> clazz) {
        WorkflowStepData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        WorkflowStepData wfstep = (WorkflowStepData)target
        def exec = wfstep.configuration
        if(strict){
            def refSe = jobExists(wfstep)

            if(!refSe){
                errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_STRICT_JOB_EXISTS)
                return
            }
        }
        if (!exec.jobName && !exec.uuid) {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_JOB_NAME_BLANK)
            return
        }
        //The original validation logic set the jobProject if it didn't exist in the config
        //we are requiring the jobProject to be set before validation
        if(exec.jobProject){
            if(authProjects && !authProjects.contains(exec.jobProject) && strict){
                errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_REF_JOB_UNAUTH)
            }
        } else {
            errors.rejectValue('configuration', WorkflowStepConstants.ERR_CODE_JOB_PROJECT_BLANK)
        }
    }

    boolean jobExists(WorkflowStepData wfstep) {
        if (!wfstep.configuration.useName && wfstep.configuration.uuid) {
            return jobDataProvider.existsByUuid(wfstep.configuration.uuid.toString())
        } else {
            return jobDataProvider.existsByProjectAndJobNameAndGroupPath(
                    wfstep.configuration.jobProject.toString(),
                    wfstep.configuration.jobName.toString(),
                    wfstep.configuration.jobGroup?.toString()
            )
        }
    }
}

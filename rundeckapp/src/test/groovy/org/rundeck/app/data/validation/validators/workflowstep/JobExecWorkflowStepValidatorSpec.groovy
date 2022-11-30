package org.rundeck.app.data.validation.validators.workflowstep

import org.rundeck.app.WorkflowStepConstants
import org.rundeck.app.data.job.RdWorkflowStep
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import spock.lang.Specification

class JobExecWorkflowStepValidatorSpec extends Specification {
    def "Test Validation #expectedCode"() {
        when:
        def dataProvider = Mock(JobDataProvider) {
            existsByProjectAndJobNameAndGroupPath(_,_,_) >> jobExists
            existsByUuid(_) >> jobExists
        }
        def validator = new JobExecWorkflowStepValidator(dataProvider, authedProjects, strict)
        RdWorkflowStep step = new RdWorkflowStep(params)
        validator.validate(step, step.errors)

        then:
        step.errors.fieldErrors[0].code == expectedCode

        where:
        expectedCode                                        | strict  | authedProjects | jobExists  | params
        WorkflowStepConstants.ERR_CODE_STRICT_JOB_EXISTS    | true  | ["proj1", "proj2"] | false     | [configuration:[jobName:"test"]]
        WorkflowStepConstants.ERR_CODE_JOB_NAME_BLANK       | false | ["proj1", "proj2"] | false     | [configuration:[:]]
        WorkflowStepConstants.ERR_CODE_REF_JOB_UNAUTH       | true  | ["proj1", "proj2"] | true      | [configuration:[jobName:"test",jobProject:"dev1"]]
        WorkflowStepConstants.ERR_CODE_JOB_PROJECT_BLANK    | true  | ["proj1", "proj2"] | true      | [configuration:[jobName:"test"]]
    }
}

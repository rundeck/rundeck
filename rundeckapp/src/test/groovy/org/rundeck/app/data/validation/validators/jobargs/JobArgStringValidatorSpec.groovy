package org.rundeck.app.data.validation.validators.jobargs

import org.rundeck.app.data.job.RdJob
import spock.lang.Specification

class JobArgStringValidatorSpec extends Specification {
    def "Validate"() {
        when:
        JobArgStringValidator validator = new JobArgStringValidator()
        RdJob rdJob = new RdJob()
        rdJob.argString = '${DATE:TODAY}'
        validator.validate(rdJob, rdJob.errors)
        def fieldError = rdJob.errors.fieldErrors[0]

        then:
        fieldError.field == "argString"
        fieldError.code == "scheduledExecution.argString.datestamp.invalid"
    }
}

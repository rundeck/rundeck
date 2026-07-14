package rundeck.data.validation.validators.jobargs

import rundeck.data.job.RdJob
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

    def "Validate accepts format with colons"() {
        when:
        def validator = new JobArgStringValidator()
        def rdJob = new RdJob(argString: '${DATE:HH:mm:ss}')
        validator.validate(rdJob, rdJob.errors)

        then:
        rdJob.errors.fieldErrors.isEmpty()
    }

    def "Validate accepts format with valid timezone"() {
        when:
        def validator = new JobArgStringValidator()
        def rdJob = new RdJob(argString: '${DATE:HH:mm:ss:Asia/Tokyo}')
        validator.validate(rdJob, rdJob.errors)

        then:
        rdJob.errors.fieldErrors.isEmpty()
    }

    def "Validate rejects format with invalid timezone"() {
        when:
        def validator = new JobArgStringValidator()
        def rdJob = new RdJob(argString: '${DATE:HH:mm:ss:NotAZone}')
        validator.validate(rdJob, rdJob.errors)
        def fieldError = rdJob.errors.fieldErrors[0]

        then:
        fieldError.field == "argString"
        fieldError.code == "scheduledExecution.argString.datestamp.invalid"
    }
}

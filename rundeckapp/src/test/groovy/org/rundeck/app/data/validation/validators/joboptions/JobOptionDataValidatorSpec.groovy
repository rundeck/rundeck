package org.rundeck.app.data.validation.validators.joboptions

import org.rundeck.app.data.job.RdJob
import org.rundeck.app.data.job.RdOption
import org.rundeck.app.data.job.RdSchedule
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.UserService
import spock.lang.Specification

class JobOptionDataValidatorSpec extends Specification {

    JobOptionDataValidator createValidator(RdJob job) {
        def fwkSvc = Mock(FrameworkService)
        def userSvc = Mock(UserService)
        new JobOptionDataValidator(fwkSvc, userSvc, job)
    }

    def "Validate"() {
        when:
        RdJob job = new RdJob()
        RdOption testOpt = new RdOption(name: "opt1", optionType: "text")
        job.optionSet = new TreeSet<>([testOpt])
        JobOptionDataValidator validator = createValidator(job)
        validator.validate(testOpt, testOpt.errors)

        then:
        testOpt.errors.errorCount == 0
    }

    //Source - services/ScheduledExecutionServiceSpec."validate scheduled job with required option without default"
    def "validate scheduled job with required option without default"() {
        given:
        RdJob job = new RdJob(scheduled: true, schedule: new RdSchedule(crontabString: '0 1 2 3 4 ? *'))
        RdOption testOpt = new RdOption(name: 'test3',
                required: true,
                enforced:false,
                defaultValue: null)
        job.optionSet = new TreeSet<>([testOpt])
        JobOptionDataValidator validator = createValidator(job)

        when:
        validator.validate(testOpt, testOpt.errors)

        then:
        testOpt.errors.hasFieldErrors('defaultValue')
    }


}

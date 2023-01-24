package rundeck.data.validation.validators.joboptions

import org.rundeck.app.core.FrameworkServiceCapabilities
import rundeck.data.job.RdJob
import rundeck.data.job.RdOption
import rundeck.data.job.RdSchedule
import spock.lang.Specification

class JobOptionDataValidatorSpec extends Specification {

    JobOptionDataValidator createValidator(RdJob job) {
        def fwkSvc = Mock(FrameworkServiceCapabilities)
        new JobOptionDataValidator(fwkSvc, job)
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

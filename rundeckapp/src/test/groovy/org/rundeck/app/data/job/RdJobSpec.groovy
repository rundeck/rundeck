package org.rundeck.app.data.job

import grails.util.Holders
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import rundeck.data.job.RdJob
import rundeck.data.job.RdOption
import rundeck.data.job.RdSchedule
import rundeck.services.FrameworkService
import rundeck.services.UserService
import spock.lang.Specification
import spock.lang.Unroll

class RdJobSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {
        { ->
            userService(InstanceFactoryBean, Mock(UserService))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
        }
    }

    def setup() {
        Holders.setGrailsApplication(grailsApplication)
    }

    def "Validation tests"() {
        when:
        RdJob job = new RdJob()
        job.scheduled = false
        job.project = "proj1#"
        job.argString = '-opt1 ${DATE:today}'

        def valid = job.validate()

        then:
        !valid
        job.errors.getFieldErrors("jobName").any {it.codes.contains("rdJob.jobName.nullable.error") }
        job.errors.getFieldErrors("workflow").any { it.codes.contains("rdJob.workflow.nullable.error") }
        job.errors.getFieldErrors("argString").any {it.code == "scheduledExecution.argString.datestamp.invalid"}
        job.errors.getFieldErrors("project").any { it.codes.contains("rdJob.project.matches.invalid") }
        job.errors.getFieldErrors("project").any { it.codes.contains("scheduledExecution.project.invalid.message") }
    }

    //Source - services/ScheduledExecutionServiceSpec."invalid options data"
    def "invalid options data"() {
        given:
        RdJob job = new RdJob()
        RdOption testOpt = new RdOption(name: name,
                defaultValue: defval,
                enforced: enforced,
                realValuesUrl: new URL(valuesUrl))
        job.optionSet = new TreeSet<>([testOpt])

        when:
        job.validate()

        then:
        job.errors.hasFieldErrors("optionSet[0].${fieldName}")

        where:
        name| defval     | enforced | valuesUrl               | fieldName
        null | 'val3'    | false    | 'http://test.com/test3' | 'name'
    }

    //Source - services/ScheduledExecutionServiceSpec."invalid options multivalued"
    def "invalid options multivalued"() {
        given:
        RdJob job = new RdJob()
        RdOption testOpt = new RdOption([name: 'test3',
                defaultValue: 'val1,val2',
                enforced: true,
                multivalued:true,
                delimiter: ',',
                valuesList: 'val1,val2,val3']+data)
        job.optionSet = new TreeSet<>([testOpt])

        when:
        job.validate()

        then:
        job.errors.hasFieldErrors("optionSet[0].${fieldName}")

        where:
        data                             | fieldName
        [delimiter: null]                | 'delimiter'
        [defaultValue: 'val1,val2,val4'] | 'defaultValue'
        [secureInput: true]              | 'multivalued'
    }

    //Source - services/ScheduledExecutionServiceSpec."do update job invalid crontab"
    @Unroll("invalid crontab value for #reason")
    def "job validation - invalid crontab"(){
        given:
        RdJob job = new RdJob()
        job.scheduled = true
        job.schedule = new RdSchedule(crontabString: crontabString)

        when:
        job.validate()

        then:
        job.errors.hasFieldErrors('schedule.crontabString')

        where:
        crontabString                   | reason
        '0 0 2 32 */6 ?'                | 'day of month'
        '0 0 2 ? 12 8'                  | 'day of week'
        '0 21 */4 */4 */6 3 2010-2040'  | 'day of month and week set'
        '0 21 */4 ? */6 ? 2010-2040'    | 'day of month and week ?'
        '0 0 25 */4 */6 ?'              | 'hour'
        '0 70 */4 */4 */6 ?'            | 'minute'
        '0 0 2 3 13 ?'                  | 'month'
        '70 21 */4 */4 */6 ?'           | 'seconds'
        '0 21 */4 */4 */6 ? z2010-2040' | 'year char'
        '0 21 */4 */4 */6'              | 'too few components'
        '0 0 2 ? 12 1975'               | 'will never fire'
    }


    def "Validate timezone"() {
        when:
        RdJob job = new RdJob()
        job.jobName = "job1"
        job.timeZone = tz
        job.validate()

        then:
        job.errors.hasFieldErrors("timeZone") == expectedErr

        where:
        tz      | expectedErr
        "GMT"   | false
        "GWT"   | true
    }
}

package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import rundeck.ScheduledExecution
import spock.lang.Specification
import spock.lang.Unroll

class RdJobServiceSpec extends Specification implements ServiceUnitTest<RdJobService>, DataTest {

    @Unroll
    def "getJobByIdOrUuid id=#id"() {
        given:
        service.jobDataProvider = Mock(JobDataProvider)
        Long lid = null
        try {
            lid = id.toLong()
        } catch(NumberFormatException e){}

        when:
        def job = service.getJobByIdOrUuid(id)

        then:
        job != null
        jobByIdCount * service.jobDataProvider.get(lid) >> jobFoundById
        jobByUuidCount * service.jobDataProvider.findByUuid(id.toString()) >> jobFoundByUuid



        where:
        jobByIdCount | jobByUuidCount | jobFoundById              | jobFoundByUuid               | id
                   1 |              0 | new ScheduledExecution()  | null                         | 1L
                   1 |              0 | new ScheduledExecution()  | null                         | "1"
                   1 |              1 | null                      | new ScheduledExecution()     | "23234234" //a fake uuid put in by a user that parses as a number
                   0 |              1 | null                      | new ScheduledExecution()     | "89F375E0-7096-4490-8265-4F94793BEC2F"


    }

}

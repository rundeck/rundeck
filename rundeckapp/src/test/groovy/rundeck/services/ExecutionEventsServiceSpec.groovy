package rundeck.services

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import rundeck.events.RdExecutionCompleteEvent
import spock.lang.Specification

class ExecutionEventsServiceSpec extends Specification implements ServiceUnitTest<ExecutionEventsService>, DataTest {
    def "On RdExecutionCompleteEvent"() {
        given:
        service.logFileStorageService = Mock(LogFileStorageService)

        when:
        service.executionComplete(new RdExecutionCompleteEvent(executionUuid: '123'))

        then:
        1 * service.logFileStorageService.submitForStorage('123')

    }
}

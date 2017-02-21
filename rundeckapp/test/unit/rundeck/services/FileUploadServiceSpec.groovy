package rundeck.services

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.JobFileRecord
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FileUploadService)
@Mock([JobFileRecord, Execution])
class FileUploadServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "create record"() {
        given:
        UUID uuid = UUID.randomUUID()
        String jobid = 'abjobid'
        String user = 'auser'
        service.configurationService = Mock(ConfigurationService) {
            getString('fileupload.plugin.type', _) >> { it[1] }
        }
        when:
        def result = service.createRecord('abcd', 123, uuid, jobid, user)
        then:
        result.storageReference == 'abcd'
        result.uuid == uuid.toString()
        result.jobId == jobid
        result.user == user
        result.size == 123L
        result.recordType == FileUploadService.RECORD_TYPE_OPTION_INPUT
        result.expirationDate == null
        result.execution == null
        result.retained == false
        result.available == true
        result.storageType == 'filesystem-file-upload'

    }
}

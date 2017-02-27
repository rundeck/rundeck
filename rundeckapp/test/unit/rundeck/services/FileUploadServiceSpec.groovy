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
            getLong('fileUploadService.tempfile.expiration', _) >> delay
        }
        service.frameworkService=Mock(FrameworkService){

        }
        Date expiryStart = new Date()
        String origName = 'afile'
        String optionName = 'myopt'
        String sha = 'z'*64
        when:
        def result = service.createRecord('abcd', 123, uuid, sha, origName, jobid, optionName, user, expiryStart)
        then:
        result.storageReference == 'abcd'
        result.uuid == uuid.toString()
        result.jobId == jobid
        result.user == user
        result.size == 123L
        result.recordType == FileUploadService.RECORD_TYPE_OPTION_INPUT
        result.recordName == 'myopt'
        result.expirationDate == new Date(expiryStart.time + delay)
        result.execution == null
        result.fileState == 'temp'
        result.storageType == 'filesystem-temp'
        result.fileName == 'afile'

        where:
        delay  | _
        0l     | _
        30000l | _
    }
}

package rundeck

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.services.FileUploadService
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.domain.DomainClassUnitTestMixin} for usage instructions
 */
@TestFor(JobFileRecord)
@Mock([Execution, Workflow, CommandExec])
class JobFileRecordSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    def "invalid state changes"() {
        given:
        def jfr = new JobFileRecord()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        jfr.stateExpired()
        then:
        IllegalStateException e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        jfr.stateExpired()
        then:
        e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        jfr.stateRetained()
        then:
        e = thrown()

        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        jfr.stateRetained()
        then:
        e = thrown()
        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        jfr.stateDeleted()
        then:
        e = thrown()

    }

    void "valid state changes"() {
        given:
        def jfr = new JobFileRecord()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.state(JobFileRecord.STATE_TEMP)
        then:
        jfr.stateIsTemp()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateRetained()
        then:
        jfr.stateIsRetained()
        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        then:
        jfr.canBecomeRetained()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateExpired()
        then:
        jfr.stateIsExpired()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        then:
        jfr.canBecomeRetained()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        jfr.stateRetained()
        then:
        jfr.stateIsRetained()

        when:
        jfr.fileState = JobFileRecord.STATE_RETAINED
        jfr.stateDeleted()
        then:
        jfr.stateIsDeleted()

        when:
        jfr.fileState = JobFileRecord.STATE_TEMP
        jfr.stateDeleted()
        then:
        jfr.stateIsDeleted()

        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        jfr.stateDeleted()
        then:
        jfr.stateIsDeleted()
        when:
        jfr.fileState = JobFileRecord.STATE_DELETED
        then:
        !jfr.canBecomeRetained()

        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        jfr.stateExpired()
        then:
        jfr.stateIsExpired()

        when:
        jfr.fileState = JobFileRecord.STATE_EXPIRED
        then:
        !jfr.canBecomeRetained()
    }

    def "tomap"() {
        given:
        def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'true'
        ).save(flush: true)
        def fileid = UUID.randomUUID().toString()
        def serveruuid = UUID.randomUUID().toString()
        def jobid = UUID.randomUUID().toString()
        def dateexpires = new Date()
        def jfr = new JobFileRecord(
                fileName: 'afilename',
                size: 123,
                recordType: FileUploadService.RECORD_TYPE_OPTION_INPUT,
                expirationDate: dateexpires,
                fileState: JobFileRecord.STATE_TEMP,
                uuid: fileid,
                serverNodeUUID: serveruuid,
                sha: '0' * 64,
                jobId: jobid,
                recordName: 'aname',
                storageType: 'atype',
                user: 'auser',
                storageReference: 'someref',
                execution: exec,
                project: 'testproj1'
        ).save(flush: true)

        when:
        def map = jfr.toMap()

        then:
        jfr != null
        map.'execId' == exec.id
        map.'uuid' == fileid
        map.'recordName' == 'aname'
        map.'jobId' == jobid
        map.'fileName' == 'afilename'
        map.'sha' == ('0' * 64)
        map.'size' == 123
        map.'expirationDate' == dateexpires
        map.'user' == 'auser'
        map.'fileState' == 'temp'
        map.'storageReference' == 'someref'
        map.'storageType' == 'atype'
        map.'storageMeta' == null
        map.'serverNodeUUID' == serveruuid
        map.'recordType' == FileUploadService.RECORD_TYPE_OPTION_INPUT
        map.'project' == 'testproj1'
    }

    def "frommap"() {
        given:
        def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                workflow: wf,
                project: "test",
                user: "user",
                status: 'true'
        ).save(flush: true)
        def fileid = UUID.randomUUID().toString()
        def serveruuid = UUID.randomUUID().toString()
        def jobid = UUID.randomUUID().toString()
        def dateexpires = new Date()

        when:
        def jfr = JobFileRecord.fromMap(
                fileName: 'afilename',
                size: 123,
                recordType: FileUploadService.RECORD_TYPE_OPTION_INPUT,
                expirationDate: dateexpires,
                fileState: JobFileRecord.STATE_TEMP,
                uuid: fileid,
                serverNodeUUID: serveruuid,
                sha: '0' * 64,
                jobId: jobid,
                recordName: 'aname',
                storageType: 'atype',
                user: 'auser',
                storageReference: 'someref',
                execution: exec,
                project: 'testproj1'
        )

        then:
        jfr != null
        jfr.execution != null
        jfr.execution.id == exec.id
        jfr.'uuid' == fileid
        jfr.'recordName' == 'aname'
        jfr.'jobId' == jobid
        jfr.'fileName' == 'afilename'
        jfr.'sha' == ('0' * 64)
        jfr.'size' == 123L
        jfr.'expirationDate' == dateexpires
        jfr.'user' == 'auser'
        jfr.'fileState' == 'temp'
        jfr.'storageReference' == 'someref'
        jfr.'storageType' == 'atype'
        jfr.'storageMeta' == null
        jfr.'serverNodeUUID' == serveruuid
        jfr.'recordType' == FileUploadService.RECORD_TYPE_OPTION_INPUT
        jfr.'project' == 'testproj1'
    }
}

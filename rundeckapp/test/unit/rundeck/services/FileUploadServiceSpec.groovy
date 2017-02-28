package rundeck.services

import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.JobFileRecord
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(FileUploadService)
@Mock([JobFileRecord, Execution, ScheduledExecution, Workflow, Option, CommandExec])
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

    def "attach file for execution"() {
        given:
        UUID uuid = UUID.randomUUID()
        String ref = uuid.toString()
        String jobid = 'abjobid'
        String user = 'auser'
        service.configurationService = Mock(ConfigurationService) {
            getString('fileupload.plugin.type', _) >> { it[1] }
            getLong('fileUploadService.tempfile.expiration', _) >> 30000L
        }
        service.frameworkService = Mock(FrameworkService) {

        }
        service.pluginService = Mock(PluginService)

        String origName = 'afile'
        String optionName = 'myopt'
        String sha = 'fc4b5fd6816f75a7c81fc8eaa9499d6a299bd803397166e8c4cf9280b801d62c'
        def jfr = new JobFileRecord(
                fileName: origName,
                size: 123,
                recordType: 'option',
                expirationDate: new Date(),
                fileState: JobFileRecord.STATE_TEMP,
                uuid: uuid.toString(),
                serverNodeUUID: null,
                sha: sha,
                jobId: jobid,
                recordName: optionName,
                storageType: 'filesystem-temp',
                user: user,
                storageReference: 'abcd'
        ).save()

        ScheduledExecution job = mkjob(jobid)
        job.validate()
        Execution exec = mkexec(job)
        exec.validate()

        when:
        def result = service.attachFileForExecution(ref, exec, optionName)
        then:
        result != null
        1 * service.pluginService.getPlugin('filesystem-temp', FileUploadPlugin) >> Mock(FileUploadPlugin) {
            1 * hasFile(ref) >> true
            1 * retrieveLocalFile(ref) >> null
            1 * retrieveFile(ref, _) >> {
                it[1].write('abcd\n'.bytes)
                5L
            }
        }
        result[0].text == 'abcd\n'
        result[1].id == jfr.id
        result[1].stateIsRetained()
        result[1].execution == exec


    }


    def "attach file for execution incorrect option/job"() {
        given:
        UUID uuid = UUID.randomUUID()
        String ref = uuid.toString()
        String jobid = 'ajobid'
        String user = 'auser'
        service.configurationService = Mock(ConfigurationService) {
            getString('fileupload.plugin.type', _) >> { it[1] }
            getLong('fileUploadService.tempfile.expiration', _) >> 30000L
        }
        service.frameworkService = Mock(FrameworkService) {

        }
        service.pluginService = Mock(PluginService)

        String origName = 'afile'
        String optionName = 'myopt'
        String sha = 'fc4b5fd6816f75a7c81fc8eaa9499d6a299bd803397166e8c4cf9280b801d62c'
        def jfr = new JobFileRecord(
                fileName: origName,
                size: 123,
                recordType: 'option',
                expirationDate: new Date(),
                fileState: JobFileRecord.STATE_TEMP,
                uuid: uuid.toString(),
                serverNodeUUID: null,
                sha: sha,
                jobId: inputjobid,
                recordName: optionName,
                storageType: 'filesystem-temp',
                user: user,
                storageReference: 'abcd'
        ).save()

        ScheduledExecution job = mkjob(jobid)
        job.validate()
        Execution exec = mkexec(job)
        exec.validate()

        when:
        def result = service.attachFileForExecution(ref, exec, inputoptname)
        then:
        FileUploadServiceException exc = thrown()
        exc.message.contains "File ref \"$uuid\" is not a valid for job $jobid, option $inputoptname"


        where:
        inputjobid   | inputoptname
        'wrongjobid' | 'myopt'
        'ajobid'     | 'wrongopt'
        'wronglgle'  | 'wrongopt'

    }

    Execution mkexec(final ScheduledExecution scheduledExecution) {
        def exec = new Execution(
                user: "testuser", project: "testproj", loglevel: 'WARN',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save(),
                scheduledExecution: scheduledExecution
        )
        exec.validate()
        if (exec.errors.hasErrors()) {
            System.err.println(exec.errors.allErrors*.toString())
        }
        exec.save(flush: true)
    }

    ScheduledExecution mkjob(jobid) {
        new ScheduledExecution(
                uuid: jobid,
                jobName: 'monkey1', project: 'testproj', description: 'blah',
                workflow: new Workflow(
                        commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                ).save(),
                options: [
                        new Option(optionType: 'file', name: 'myopt', required: false, enforced: false).save()
                ]
        ).save(flush: true)
    }
}

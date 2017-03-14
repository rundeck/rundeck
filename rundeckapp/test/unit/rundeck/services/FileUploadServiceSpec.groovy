package rundeck.services

import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
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
import spock.lang.Unroll

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
        service.taskService = Mock(TaskService)

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
        result.id == jfr.id
        result.stateIsRetained()
        result.execution == exec


    }

    def "loadFileOptionInputs"() {
        given:
        UUID uuid = UUID.randomUUID()
        String ref = uuid.toString()
        String storageRef = 'astorageref'
        String jobid = 'abjobid'
        String user = 'auser'
        service.configurationService = Mock(ConfigurationService) {
            getString('fileupload.plugin.type', _) >> { it[1] }
            getLong('fileUploadService.tempfile.expiration', _) >> 30000L
        }
        service.frameworkService = Mock(FrameworkService) {

        }
        service.pluginService = Mock(PluginService)
        service.taskService = Mock(TaskService)

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
                storageReference: storageRef
        ).save()

        ScheduledExecution job = mkjob(jobid)
        job.validate()
        Execution exec = mkexec(job)
        exec.validate()
        StepExecutionContext context = Mock(StepExecutionContext)

        when:
        def result = service.loadFileOptionInputs(exec, job, context)
        then:
        result != null
        1 * service.pluginService.getPlugin('filesystem-temp', FileUploadPlugin) >> Mock(FileUploadPlugin) {
            1 * hasFile(storageRef) >> true
            1 * retrieveLocalFile(storageRef) >> null
            1 * retrieveFile(storageRef, _) >> {
                it[1].write('abcd\n'.bytes)
                5L
            }
        }
        1 * context.getDataContext() >> [
                option: [
                        (optionName): ref
                ]
        ]
        1 * context.getExecutionListener() >> Mock(ExecutionListener)
        result[optionName] != null
        result[optionName + '.fileName'] != null
        result[optionName + '.sha'] == jfr.sha


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

    @Unroll
    def "validate uuid for job option incorrect option/job"() {
        given:
        String jobid = 'ajobid'
        String user = 'auser'
//        service.configurationService = Mock(ConfigurationService) {
//            getString('fileupload.plugin.type', _) >> { it[1] }
//            getLong('fileUploadService.tempfile.expiration', _) >> 30000L
//        }
//        service.frameworkService = Mock(FrameworkService) {
//
//        }
//        service.pluginService = Mock(PluginService)

        String origName = 'afile'
        String optionName = 'myopt'
        String sha = 'fc4b5fd6816f75a7c81fc8eaa9499d6a299bd803397166e8c4cf9280b801d62c'
        def jfr = new JobFileRecord(
                fileName: origName,
                size: 123,
                recordType: 'option',
                expirationDate: new Date(),
                fileState: state,
                uuid: '44a26bb3-5013-4906-9997-286306005408',
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

        when:
        def result = service.validateFileRefForJobOption(ref, inputjobid, inputoptname)
        then:
        result.valid == false
        result.error == errorCode
        result.args == errorArgs


        where:
        state     | ref                                    | inputjobid   | inputoptname | errorCode | errorArgs
        'deleted' | '44a26bb3-5013-4906-9997-286306005408' |
                'ajobid'                                                  |
                'myopt'                                                                  |
                'state'                                                                              |
                ['44a26bb3-5013-4906-9997-286306005408', 'deleted']
        'temp'    | 'wrong'                                |
                'blah'                                                    |
                'blah2'                                                                  |
                'notfound'                                                                           |
                ['wrong', 'blah', 'blah2']
        'temp'    | '44a26bb3-5013-4906-9997-286306005408' | 'wrongjobid' |
                'myopt'                                                                  |
                'invalid'                                                                            |
                ['44a26bb3-5013-4906-9997-286306005408', 'wrongjobid', 'myopt']
        'temp'    | '44a26bb3-5013-4906-9997-286306005408' | 'ajobid'     |
                'wrongopt'                                                               |
                'invalid'                                                                            |
                ['44a26bb3-5013-4906-9997-286306005408', 'ajobid', 'wrongopt']
        'temp'    | '44a26bb3-5013-4906-9997-286306005408' | 'wronglgle'  |
                'wrongopt'                                                               |
                'invalid'                                                                            |
                ['44a26bb3-5013-4906-9997-286306005408', 'wronglgle', 'wrongopt']

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

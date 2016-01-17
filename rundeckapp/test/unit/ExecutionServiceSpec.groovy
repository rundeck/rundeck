import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.storage.KeyStorageTree
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService
import rundeck.services.JobStateService
import rundeck.services.LogFileStorageService
import rundeck.services.ReportService
import rundeck.services.StorageService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/17/15.
 */
@TestFor(ExecutionService)
@Mock([Execution,ScheduledExecution,Workflow,CommandExec,Option,ExecReport,LogFileStorageRequest])
class ExecutionServiceSpec extends Specification {

    void "retry execution otherwise running"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'userB',
                    project: 'AProject'
        ).save()
        def exec2 = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec2.id)

        then:
        ExecutionServiceException e=thrown()
        e.code=='conflict'
        e.message ==~ /.*is currently being executed.*/
    }
    void "retry execution new execution"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec.id)

        then:
        e2!=null
    }

    @Unroll
    def "log execution state"(String statusString, String resultStatus, boolean issuccess,boolean iscancelled,boolean istimedout,boolean willretry) {
        given:
        def params = [:]
        service.reportService = Stub(ReportService) {
            reportExecutionResult(_) >> { args ->
                params = args[0]
            }
        }
        when:
        service.logExecution(
                null,
                'test1',
                'user1',
                issuccess,
                statusString,
                11,
                new Date(),
                'abc',
                'job1',
                'blah',
                iscancelled,
                istimedout,
                willretry,
                '1/1/1',
                null
        )

        then:
        params.jcExecId == 11
        params.jcJobId == 'abc'
        params.reportId == 'job1'
        params.adhocExecution == false
        params.ctxProject == 'test1'
        params.author == 'user1'
        params.title == 'blah'
        params.status == resultStatus
        params.node == '1/1/1'
        params.message == "Job status ${statusString}"
        params.dateStarted != null
        params.dateCompleted != null

        where:
        statusString        | resultStatus | issuccess | iscancelled | istimedout | willretry
        'succeeded'         | 'succeed'    | true      | false       | false      | false
        'true'              | 'succeed'    | true      | false       | false      | false
        'custom'            | 'other'      | false     | false       | false      | false
        'other status'      | 'other'      | false     | false       | false      | false
        'false'             | 'fail'       | false     | false       | false      | false
        'failed'            | 'fail'       | false     | false       | false      | false
        'aborted'           | 'cancel'     | false     | true        | false      | false
        'timedout'          | 'timeout'    | false     | false       | true       | false
        'failed-with-retry' | 'retry'      | false     | false       | false      | true
    }

    def "createJobReferenceContext secure opts blank values"(){
        given:
        def context = ExecutionContextImpl.builder()
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'secureOption':[:],'job':['execid':'123']])
                                          .privateDataContext(['option':[:],])
                                          .user('aUser')
                                          .build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        null != se
        def opt1 = new Option(name: 'test1',  enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2',  enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService=Mock(FrameworkService){
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt=service.createJobReferenceContext(
                se,
                context,
                ['-test1','${option.test1}','-test2','${option.test2}'] as String[],
                null,null,null, null, null,false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': '']
        newCtxt.dataContext['option'] == ['test2': '']
        newCtxt.privateDataContext['option'] == ['test1': '']
    }
    def "createJobReferenceContext secure opts default storage path values should be read from storage"(){
        given:
        def context = ExecutionContextImpl.builder()
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'secureOption':[:],'job':['execid':'123']])
                                          .privateDataContext(['option':[:],])
                                          .user('aUser')
                                          .build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        null != se
        def opt1 = new Option(
                name: 'test1',
                enforced: false,
                required: false,
                secureInput: true,
                defaultStoragePath: 'keys/test1'
        )
        def opt2 = new Option(
                name: 'test2',
                enforced: false,
                required: false,
                secureInput: true,
                secureExposed: true,
                defaultStoragePath: 'keys/test2')
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService=Mock(FrameworkService){
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)

        when:

        def newCtxt=service.createJobReferenceContext(
                se,
                context,
                [] as String[],//null values for the input options
                null,null,null, null, null,false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': 'newtest2']
        newCtxt.dataContext['option'] == ['test2': 'newtest2']
        newCtxt.privateDataContext['option'] == ['test1': 'newtest1']

        service.storageService.storageTreeWithContext(_) >> Mock(KeyStorageTree) {
            1 * readPassword('keys/test1') >> {
                    return 'newtest1'.bytes
            }
            1 * readPassword('keys/test2') >> {
                    return 'newtest2'.bytes
            }
        }
    }
    def "createJobReferenceContext secure opts replacement values"(){
        given:
        def context = ExecutionContextImpl.builder()
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'secureOption':['test2':'zimbo'],'job':['execid':'123']])
                                          .privateDataContext(['option':['zilch':'phoenix'],])
                                          .user('aUser')
                                          .build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        null != se
        def opt1 = new Option(name: 'test1',  enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2',  enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService=Mock(FrameworkService){
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt=service.createJobReferenceContext(
                se,
                context,
                ['-test1','${option.zilch}','-test2','${option.test2}'] as String[],
                null,null,null, null, null,false
        )

        then:

        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo']
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']
    }

    def "delete execution unauthorized"(){
        given:

        service.frameworkService=Mock(FrameworkService)
        def auth=Mock(AuthContext)
        def execution = new Execution()

        when:
        def result=service.deleteExecution(execution,auth,'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> false
        !result.success
        result.error=='unauthorized'

    }
    def "delete execution running"(){
        given:

        service.frameworkService=Mock(FrameworkService)
        def auth=Mock(AuthContext)
        def execution = new Execution()
        execution.dateStarted = new Date()

        when:
        def result=service.deleteExecution(execution,auth,'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> true

        !result.success
        result.error=='running'
    }
    def "delete execution files"(){
        given:

        service.frameworkService=Mock(FrameworkService)
        def auth=Mock(AuthContext)
        def execution = new Execution(
                user:'userB',
                project: 'AProject',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        execution.dateStarted = new Date()
        execution.dateCompleted = new Date()
        execution.status='succeeded'

        def file1 = File.createTempFile("ExecutionServiceSpec-test", "file")
        file1.deleteOnExit()
        def file2 = File.createTempFile("ExecutionServiceSpec-test", "file")
        file2.deleteOnExit()


        service.logFileStorageService=Mock(LogFileStorageService){
            1 * getFileForExecutionFiletype(execution, 'rdlog', true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', true) >> file2
            0 * _(*_)
        }


        when:
        def result=service.deleteExecution(execution,auth,'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> true

        result.success

        !file1.exists()
        !file2.exists()
    }
    def "delete execution files failure"(){
        given:

        service.frameworkService=Mock(FrameworkService)
        def auth=Mock(AuthContext)
        def execution = new Execution(
                user:'userB',
                project: 'AProject',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        execution.dateStarted = new Date()
        execution.dateCompleted = new Date()
        execution.status='succeeded'

        def file1 = Mock(File){
            exists()>>true
            delete()>>false
            isDirectory() >> false
        }

        service.logFileStorageService=Mock(LogFileStorageService){
            1 * getFileForExecutionFiletype(execution, 'rdlog', true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', true)
            0 * _(*_)
        }


        when:
        def result=service.deleteExecution(execution,auth,'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> true

        result.success
    }

    def "loadSecureOptionStorageDefaults"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec(
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                )
                        ]
                ),
                options: [
                        new Option(
                                name: 'opt1',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: 'keys/opt1'
                        ),
                        new Option(
                                name: 'opt2',
                                secureInput: true,
                                secureExposed: true,
                                defaultStoragePath: 'keys/opt2'
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        secureOptsExposed.putAll(inputExposed)
        Map secureOpts = [:]
        secureOpts.putAll(inputSecure)
        def authContext = Mock(AuthContext)
        service.storageService = Mock(StorageService)


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext)

        then:
        1 * service.storageService.storageTreeWithContext(authContext) >> Mock(KeyStorageTree) {
            readPassword('keys/opt1') >> {
                if (hasopt1 && readopt1) {
                    return 'newopt1'.bytes
                }
                if (!readopt1) {
                    throw new StorageException(
                            "unauthorized",
                            StorageException.Event.READ,
                            PathUtil.asPath('keys/opt1')
                    )
                }
                if (!hasopt1) {
                    throw new StorageException(
                            "not found",
                            StorageException.Event.READ,
                            PathUtil.asPath('keys/opt1')
                    )
                }
            }
            readPassword('keys/opt2') >> {
                if (hasopt2 && readopt2) {
                    return 'newopt2'.bytes
                }
                if (!readopt2) {
                    throw new StorageException(
                            "unauthorized",
                            StorageException.Event.READ,
                            PathUtil.asPath('keys/opt2')
                    )
                }
                if (!hasopt2) {
                    throw new StorageException(
                            "not found",
                            StorageException.Event.READ,
                            PathUtil.asPath('keys/opt2')
                    )
                }
            }
        }
        opt1result == secureOpts['opt1']
        opt2result == secureOptsExposed['opt2']

        where:
        inputExposed    | inputSecure     | opt1result | opt2result | hasopt1 | readopt1 | hasopt2 | readopt2
        [:]             | [:]             | null       | 'newopt2'  | true    | false    | true    | true
        [:]             | [:]             | null       | 'newopt2'  | false   | true     | true    | true

        [:]             | [:]             | 'newopt1'  | null       | true    | true     | false   | true
        [:]             | [:]             | 'newopt1'  | null       | true    | true     | true    | false

        [:]             | [:]             | 'newopt1'  | 'newopt2'  | true    | true     | true    | true
        [opt2: 'aval2'] | [:]             | 'newopt1'  | 'aval2'    | true    | true     | true    | true
        [:]             | [opt1: 'aval1'] | 'aval1'    | 'newopt2'  | true    | true     | true    | true

    }
}

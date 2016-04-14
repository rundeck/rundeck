import com.dtolabs.rundeck.app.support.QueueQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.storage.KeyStorageTree
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import org.springframework.context.MessageSource
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.ExecutionServiceValidationException
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
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, Option, ExecReport, LogFileStorageRequest])
class ExecutionServiceSpec extends Specification {

    void "retry execution otherwise running"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        def exec = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        def exec2 = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'AProject'
        ).save()
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, ['extra.option.test': '12'], true, exec2.id)

        then:
        ExecutionServiceException e = thrown()
        e.code == 'conflict'
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
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        def exec = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'AProject'
        ).save()
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, ['extra.option.test': '12'], true, exec.id)

        then:
        e2 != null
    }

    @Unroll
    def "log execution state"(
            String statusString,
            String resultStatus,
            boolean issuccess,
            boolean iscancelled,
            boolean istimedout,
            boolean willretry
    )
    {
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

    def "createJobReferenceContext secure opts blank values"() {
        given:
        def context = ExecutionContextImpl.builder()
                                          .
                threadCount(1)
                                          .
                keepgoing(false)
                                          .
                dataContext(['option': ['monkey': 'wakeful'], 'secureOption': [:], 'job': ['execid': '123']])
                                          .
                privateDataContext(['option': [:],])
                                          .
                user('aUser')
                                          .
                build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        null != se
        def opt1 = new Option(name: 'test1', enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2', enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                context,
                ['-test1', '${option.test1}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': '']
        newCtxt.dataContext['option'] == ['test2': '']
        newCtxt.privateDataContext['option'] == ['test1': '']
    }

    def "createJobReferenceContext secure opts default storage path values should be read from storage"() {
        given:
        def context = ExecutionContextImpl.builder()
                                          .
                threadCount(1)
                                          .
                keepgoing(false)
                                          .
                dataContext(['option': ['monkey': 'wakeful'], 'secureOption': [:], 'job': ['execid': '123']])
                                          .
                privateDataContext(['option': [:],])
                                          .
                user('aUser')
                                          .
                build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
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
                defaultStoragePath: 'keys/test2'
        )
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                context,
                [] as String[],//null values for the input options
                null, null, null, null, null, false
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

    def "createJobReferenceContext secure opts replacement values"() {
        given:
        def context = ExecutionContextImpl.builder()
                                          .
                threadCount(1)
                                          .
                keepgoing(false)
                                          .
                dataContext(
                        ['option': ['monkey': 'wakeful'], 'secureOption': ['test2': 'zimbo'], 'job': ['execid': '123']]
                )
                                          .
                privateDataContext(['option': ['zilch': 'phoenix'],])
                                          .
                user('aUser')
                                          .
                build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        null != se
        def opt1 = new Option(name: 'test1', enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2', enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                context,
                ['-test1', '${option.zilch}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, false
        )

        then:

        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo']
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']
    }

    def "delete execution unauthorized"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        def auth = Mock(AuthContext)
        def execution = new Execution()

        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> false
        !result.success
        result.error == 'unauthorized'

    }

    def "delete execution running"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        def auth = Mock(AuthContext)
        def execution = new Execution()
        execution.dateStarted = new Date()

        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> true

        !result.success
        result.error == 'running'
    }

    def "delete execution files"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        def auth = Mock(AuthContext)
        def execution = new Execution(
                user: 'userB',
                project: 'AProject',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        execution.dateStarted = new Date()
        execution.dateCompleted = new Date()
        execution.status = 'succeeded'

        def file1 = File.createTempFile("ExecutionServiceSpec-test", "file")
        file1.deleteOnExit()
        def file2 = File.createTempFile("ExecutionServiceSpec-test", "file")
        file2.deleteOnExit()


        service.logFileStorageService = Mock(LogFileStorageService) {
            1 * getFileForExecutionFiletype(execution, 'rdlog', true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', true) >> file2
            0 * _(*_)
        }


        when:
        def result = service.deleteExecution(execution, auth, 'bob')

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

    def "delete execution files failure"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        def auth = Mock(AuthContext)
        def execution = new Execution(
                user: 'userB',
                project: 'AProject',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        execution.dateStarted = new Date()
        execution.dateCompleted = new Date()
        execution.status = 'succeeded'

        def file1 = Mock(File) {
            exists() >> true
            delete() >> false
            isDirectory() >> false
        }

        service.logFileStorageService = Mock(LogFileStorageService) {
            1 * getFileForExecutionFiletype(execution, 'rdlog', true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', true)
            0 * _(*_)
        }


        when:
        def result = service.deleteExecution(execution, auth, 'bob')

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

    def "validate option values, required opt with default storage"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(
                name: 'opt1',
                secureInput: true,
                secureExposed: false,
                defaultStoragePath: 'keys/opt1',
                required: true
        )
        )
        service.storageService = Mock(StorageService){
            storageTreeWithContext(_)>>Mock(KeyStorageTree){
                readPassword('keys/opt1')>>'asdf'.bytes
            }
        }

        def authContext = Mock(UserAndRolesAuthContext)
        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        expect:
        service.validateOptionValues(se, [:], authContext)

    }
    def "validate option values, required opt with default storage, value missing"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(
                name: 'opt1',
                secureInput: true,
                secureExposed: false,
                defaultStoragePath: 'keys/opt1',
                required: true
        )
        )
        service.storageService = Mock(StorageService){
            storageTreeWithContext(_)>>Mock(KeyStorageTree){
                readPassword('keys/opt1')>>{
                    throw new StorageException("bogus",StorageException.Event.READ,PathUtil.asPath('keys/opt1'))
                }
            }
        }
        def authContext = Mock(UserAndRolesAuthContext)
        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        service.validateOptionValues(se, [:], authContext)

        then:
        ExecutionServiceValidationException e = thrown()
        e.errors.containsKey('opt1')

    }
    def "validate option values, regex"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'test1', enforced: false))
        se.addToOptions(new Option(name: 'test2', enforced: false, regex: '.*abc.*'))
        se.addToOptions(new Option(name: 'test3', enforced: false, regex: 'shampoo[abc].*'))
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts                                           | _
        ['test1': 'some value']                        | _
        ['test1': 'some value', 'test2': 'abc']        | _
        ['test1': 'some value', 'test2': 'abcdefg']    | _
        ['test1': 'some value', 'test2': 'xyzabcdefg'] | _
        ['test3': 'shampooa'] | _
        ['test3': 'shampoob'] | _
        ['test3': 'shampooc'] | _
        ['test3': 'shampoocxyz234'] | _
    }
    def "validate option values, regex failure"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'test1', enforced: false))
        se.addToOptions(new Option(name: 'test2', enforced: false, regex: '.*abc.*'))
        se.addToOptions(new Option(name: 'test3', enforced: false, regex: 'shampoo[abc].*'))

        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message=='domain.Option.validation.regex.invalid'

        where:
        opts                                           | _
        ['test2': 'xyz'] | _
        ['test3': 'shampooz'] | _
    }
    def "validate option values, enforced valid"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true)
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts           | _
        ['test1': 'a'] | _
        ['test1': 'b'] | _
        ['test1': 'abc'] | _
    }
    def "validate option values, enforced invalid"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true)
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message=='domain.Option.validation.allowed.invalid'

        where:
        opts           | _
        ['test1': 'x'] | _
        ['test1': 'y'] | _
        ['test1': 'x,y'] | _
        ['test1': 'some value'] | _
    }
    def "validate option values, enforced valid multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true,multivalued: true,delimiter: ',')
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts           | _
        ['test1': 'a,b'] | _
        ['test1': ['a','b']] | _
        ['test1': 'b,'] | _
        ['test1': 'abc,a,b'] | _
        ['test1': ['abc','a','b']] | _
    }
    def "validate option values, enforced invalid multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true,multivalued: true,delimiter: ',')
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message=='domain.Option.validation.allowed.values'

        where:
        opts           | _
        ['test1': 'blah'] | _
        ['test1': 'a,blah'] | _
        ['test1': ['a','blah']] | _
        ['test1': 'blah,'] | _
        ['test1': 'abc,a,blah'] | _
        ['test1': ['abc','a','blah']] | _
    }
    def "validate option values, enforced valid multivalue regex"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true,multivalued: true,delimiter: ' ',regex: '^[abc]+$')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts           | _
        ['test1': 'abc'] | _
        ['test1': 'abc abccaba'] | _
        ['test1': ['abc']] | _
        ['test1': ['abc','abcaccab']] | _
    }
    def "validate option values, enforced invalid multivalue regex"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: false,multivalued: true,delimiter: ' ',regex: '^[abc]+$')
        option.delimiter=' '
        se.addToOptions(option)

        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message=='domain.Option.validation.regex.values'

        where:
        opts           | _
        ['test1': 'abcd'] | _
        ['test1': 'abc abccabazzz'] | _
        ['test1': ['abczz']] | _
        ['test1': ['abc','abcaccabzzz']] | _
    }
    def "validate option values, required valid"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', required: true)
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts           | _
        ['test1': 'x'] | _
        ['test1': 'y'] | _
        ['test1': 'x,y'] | _
        ['test1': 'some value'] | _
    }
    def "validate option values, required invalid"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', required: true)
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)
        final Option option2 = new Option(name: 'test2', required: true)
        se.addToOptions(option2)

        service.messageSource=Mock(MessageSource){
            getMessage(_,_,_)>>{
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message=='domain.Option.validation.required'


        where:
        opts           | missingkey
        ['test2': 'a'] | 'test1'
        ['test1': 'a'] | 'test2'
    }

    def "filter opts params string"(){
        given:
        def params=[
                'option.opt1':'abc',
                'option.opt2':'def'
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        'abc' == result.opt1
        'def' == result.opt2

    }
    def "filter opts params list"(){
        given:
        def params=[
                'option.opt1':['abc',''],
                'option.opt2':(['def','ghi'] as Set)
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        ['abc'] == result.opt1
        ['def','ghi'] == result.opt2

    }
    def "filter opts params string array"(){
        String[] strings = ['abc', '']
        String[] strings2 = ['def','ghi']
        given:
        def params=[
                'option.opt1': strings,
                'option.opt2':strings2
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        ['abc'] == result.opt1
        ['def','ghi'] == result.opt2

    }
    def "filter opts params incorrect type"(){
        given:
        def params=[
                'option.opt1': 123,
                'option.opt2':new Object(),
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        0==result.size()
        null == result.opt1
        null == result.opt2

    }
    @Unroll
    def "parse job opts from string multivalue"(){
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'opt1', enforced: false, multivalued: true, delimiter: ','))
        final opt2 = new Option(name: 'opt2', enforced: true, multivalued: true, delimiter: ' ')
        opt2.delimiter=' '
        opt2.addToValues('a')
        opt2.addToValues('b')
        opt2.addToValues('abc')
        se.addToOptions(opt2)


        when:
        def result = service.parseJobOptsFromString(se, argString)

        then:
        result==expected

        where:
        argString      | expected
        '-opt1 test'   | [opt1: ['test']]
        '-opt1 test,x' | [opt1: ['test', 'x']]
        '-opt1 \'test x\'' | [opt1: ['test x']]
        '-opt2 a'      | [opt2: ['a']]
        '-opt2 a,b'    | [opt2: ['a,b']]
        '-opt2 \'blah zah nah\''    | [opt2: ['blah','zah','nah']]


    }

    def "can read storage password"() {
        given:
        AuthContext context = Mock(AuthContext)
        service.storageService = Mock(StorageService)

        when:
        def result = service.canReadStoragePassword(context, path, false)

        then:
        service.storageService.storageTreeWithContext(context)>>Mock(KeyStorageTree){
            1 * readPassword(path)>>{
                if(throwsexception){
                    throw new StorageException(StorageException.Event.READ,PathUtil.asPath(path))
                }
                'data'.bytes
            }
        }
        result == canread

        where:
        path                 | throwsexception | canread
        'keys/test/password' | false           | true
        'keys/test/password' | true            | false

    }

    def "list now running project"() {
        given:
        def query = new QueueQuery()
        query.projFilter = 'AProject'
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        def exec2 = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'BProject'
        ).save()
        when:
        def result = service.queryQueue(query)

        then:
        1 == result.total
        1 == result.nowrunning.size()

    }
    def "list now running all projects"() {
        given:
        def query = new QueueQuery()
        query.projFilter = '*'
        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        def exec2 = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'BProject'
        ).save()
        when:
        def result = service.queryQueue(query)

        then:
        2 == result.total
        2 == result.nowrunning.size()

    }
}

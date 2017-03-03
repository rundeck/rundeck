/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.dtolabs.rundeck.app.support.QueueQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.server.plugins.storage.KeyStorageTree
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import org.springframework.context.MessageSource
import rundeck.*
import rundeck.services.*
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/17/15.
 */
@TestFor(ExecutionService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, Option, ExecReport, LogFileStorageRequest])
class ExecutionServiceSpec extends Specification {
    private Map createJobParams(Map overrides = [:]) {
        [
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ),
                serverNodeUUID: null,
                scheduled     : true
        ] + overrides
    }
    @Unroll
    def "expand date strings"() {
        given:
        def cal = new GregorianCalendar(1970, 0, 14, 8, 20, 30)
        Date thedate = cal.time

        when:
        def result = service.expandDateStrings(input, thedate)

        then:
        result == expected
        where:
        input                                          | expected
        ''                                             | ''

        '${DATE:yyyy-MM-dd}'                           | '1970-01-14'
        '${DATE:yyyy-MM-dd} blah ${DATE:yyyy-MM-dd}'   | '1970-01-14 blah 1970-01-14'
        '${DATE:yyyy-MM-dd} blah ${DATE+3:yyyy-MM-dd}' | '1970-01-14 blah 1970-01-17'
        '${DATE-7:yyyy-MM-dd}'                         | '1970-01-07'
        'invalid ${DATE-asdf7:yyyy-MM-dd}'             | 'invalid ${DATE-asdf7:yyyy-MM-dd}'
    }
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
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, null, ['extra.option.test': '12'], true, exec2.id)

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
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, null, ['extra.option.test': '12'], true, exec.id)

        then:
        e2 != null
    }

    void "create execution as user"() {

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

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, 'testuser', ['extra.option.test': '12'])

        then:
        e2 != null
        e2.user == 'testuser'
    }

    void "create execution expand date strings"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: argString,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                null
        )

        then:
        e2 != null
        e2.argString =~ resultPat

        where:
        argString                                             | resultPat
        '-opt1 blah -opt2 ${DATE:yyyy-MM-dd}'                 | /-opt1 blah -opt2 \d{4}-\d{2}-\d{2}/
        '-opt1 ${DATE+1:yyyy-MM-dd} -opt2 ${DATE:yyyy-MM-dd}' | /-opt1 \d{4}-\d{2}-\d{2} -opt2 \d{4}-\d{2}-\d{2}/
    }

    void "create execution and prep expand date strings"() {

        given:
        def params = [
                project    : 'AProject',
                groupPath  : 'some/where',
                description: 'a job',
                argString  : argString,
                user       : 'bob',
                workflow   : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry      : '1'
        ]

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        when:
        Execution e2 = service.createExecutionAndPrep(
                params,
                null
        )

        then:
        e2 != null
        e2.argString =~ resultPat

        where:
        argString                                             | resultPat
        '-opt1 blah -opt2 ${DATE:yyyy-MM-dd}'                 | /-opt1 blah -opt2 \d{4}-\d{2}-\d{2}/
        '-opt1 ${DATE+1:yyyy-MM-dd} -opt2 ${DATE:yyyy-MM-dd}' | /-opt1 \d{4}-\d{2}-\d{2} -opt2 \d{4}-\d{2}-\d{2}/
    }
    void "execute job as user"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                user: 'test1',
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
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> true
        }

        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        def result = service.executeJob(job, authContext, 'test2', [:])

        then:
        1 * service.scheduledExecutionService.scheduleTempJob(job, 'test2', authContext, _, [:], [:], 0) >> { args ->
            args[3].id
        }
        result != null
        result.success
        result.executionId != null
        result.name == job.jobName
        result.execution != null
        result.executionId == result.execution.id
        result.execution.user == 'test2'

    }

    void "execute ad-hoc scheduled job as user"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'purple',
                project: 'AProject',
                user: 'test1',
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
        def Map params  = [runAtTime: "2080-01-01T12:10:01.000+0000"]
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)

        Calendar expectCal = Calendar.getInstance()
        expectCal.set(year: 2080, month: Calendar.JANUARY, dayOfMonth: 1, hourOfDay: 12, minute: 10, second: 1)
        Date expected = expectCal.getTime()

        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> true
        }

        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }


        when:
        def result = service.scheduleAdHocJob(job, authContext, 'test2', params)

        then:
        1 * service.scheduledExecutionService.scheduleAdHocJob(*_) >> { args ->
            final Date startDate    = args[7]
            // The start time may differ slightly (milliseconds)
            assert startDate.getTime() - expected.getTime() <= 500 ||
                startDate.getTime() - expected.getTime() >= -500
            return expected
        }
        result != null
        result.success
        result.executionId != null
        result.name == job.jobName
        result.execution != null
        result.executionId == result.execution.id
        result.execution.user == 'test2'
        result.nextRun.getTime() == expected.getTime()
    }

    @Unroll
    def "log execution state"(
            String statusString,
            String resultStatus,
            boolean issuccess,
            boolean iscancelled,
            boolean istimedout,
            boolean willretry,
            String succeededList,
            String failedList,
            String filter
    ) {
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
                null,
                succeededList, 
                failedList, 
                filter
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
        params.succeededNodeList == succeededList
        params.failedNodeList == failedList
        params.filterApplied == filter

        where:
        statusString        | resultStatus | issuccess | iscancelled | istimedout | willretry | succeededList | failedList | filter
        'succeeded'         | 'succeed'    | true      | false       | false      | false     | 'nodea'       | null       | 'tags: linux'
        'true'              | 'succeed'    | true      | false       | false      | false     | 'nodea'       | 'nodeb'    | 'tags: linux'
        'custom'            | 'other'      | false     | false       | false      | false     | 'nodea'       | 'nodeb'    | '.*'
        'other status'      | 'other'      | false     | false       | false      | false     | null          | null       | null
        'false'             | 'fail'       | false     | false       | false      | false     | null          | null       | null
        'failed'            | 'fail'       | false     | false       | false      | false     | null          | null       | null
        'aborted'           | 'cancel'     | false     | true        | false      | false     | null          | null       | null
        'timedout'          | 'timeout'    | false     | false       | true       | false     | null          | null       | null
        'failed-with-retry' | 'retry'      | false     | false       | false      | true      | null          | null       | null
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
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test1', '${option.test1}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': '']
        newCtxt.dataContext['option'] == ['test2': '']
        newCtxt.privateDataContext['option'] == ['test1': '']
    }

    def "createJobReferenceContext global vars"() {
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
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            1 * getProjectGlobals(*_) >> ['a': 'b', c: 'd']
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],
                null, null, null, null, null, false
        )

        then:
        newCtxt.dataContext['globals'] == [a: 'b', c: 'd']
    }

    def "createJobReferenceContext expands DATE template strings"() {
        given:
        def context = ExecutionContextImpl.builder().with {
            threadCount 1
            keepgoing false
            dataContext(['option': ['monkey': 'wakeful'], 'secureOption': [:], 'job': ['execid': '123']])
            privateDataContext(['option': [:],])
            user 'aUser'
            build()
        }
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                options: [
                        new Option(name: 'blah', enforced: false, required: false),
                        new Option(name: 'blah2', enforced: false, required: false),
                ],
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                )
        null != se.save()
        Execution exec = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false,
                scheduledExecution: se,
                dateStarted: new Date(2015 - 1900, 02, 03, 04, 05, 06)
        )
        null != exec.save()


        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                exec,
                context,
                args as String[],
                null, null, null, null, null, false
        )

        then:
        newCtxt.dataContext['option'] == result

        where:
        args                                       | result
        ['-blah', 'xyz', '-blah2', '${DATE:yyyy}'] | ['blah': 'xyz', blah2: '2015']
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
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
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
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test1', '${option.zilch}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, false
        )

        then:

        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo']
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']
    }

    def "Create execution context with global vars"() {
        given:

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * filterAuthorizedNodes(*_)
            1 * getProjectGlobals(*_) >> [a: 'b', c: 'd']
            0 * _(*_)
        }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false
        )

        when:
        def val = service.createContext(se, null, null, null, null, null, null)
        then:
        val != null
        val.nodeSelector == null
        val.frameworkProject == "testproj"
        "testuser" == val.user
        1 == val.loglevel
        !val.executionListener
        val.dataContext.globals == [a: 'b', c: 'd']
    }

    def "Create execution context with charset"() {
        given:

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * filterAuthorizedNodes(*_)
            1 * getProjectGlobals(*_) >> [:]
            0 * _(*_)
        }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false
        )

        when:
        def val = service.createContext(se, null, null, null, null, null, null, null, null, null, charset)
        then:
        val != null
        val.charsetEncoding == charset

        where:
        charset      | _
        null         | _
        'UTF-8'      | _
        'ISO-8859-1' | _
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


        service.fileUploadService = Mock(FileUploadService)
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
        service.fileUploadService = Mock(FileUploadService)
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

    def "delete execution job file records"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        service.fileUploadService = Mock(FileUploadService)
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



        service.logFileStorageService = Mock(LogFileStorageService)


        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.frameworkService.authResourceForProject(_)
        1 * service.frameworkService.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN]
        ) >> true
        1 * service.fileUploadService.deleteRecordsForExecution(execution)

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
        service.storageService = Mock(StorageService) {
            storageTreeWithContext(_) >> Mock(KeyStorageTree) {
                readPassword('keys/opt1') >> 'asdf'.bytes
            }
        }

        def authContext = Mock(UserAndRolesAuthContext)
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
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
        service.storageService = Mock(StorageService) {
            storageTreeWithContext(_) >> Mock(KeyStorageTree) {
                readPassword('keys/opt1') >> {
                    throw new StorageException("bogus", StorageException.Event.READ, PathUtil.asPath('keys/opt1'))
                }
            }
        }
        def authContext = Mock(UserAndRolesAuthContext)
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
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
        ['test3': 'shampooa']                          | _
        ['test3': 'shampoob']                          | _
        ['test3': 'shampooc']                          | _
        ['test3': 'shampoocxyz234']                    | _
    }

    def "validate option values, regex failure"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'test1', enforced: false))
        se.addToOptions(new Option(name: 'test2', enforced: false, regex: '.*abc.*'))
        se.addToOptions(new Option(name: 'test3', enforced: false, regex: 'shampoo[abc].*'))

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == 'domain.Option.validation.regex.invalid'

        where:
        opts                  | _
        ['test2': 'xyz']      | _
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
        opts             | _
        ['test1': 'a']   | _
        ['test1': 'b']   | _
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

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == 'domain.Option.validation.allowed.invalid'

        where:
        opts                    | _
        ['test1': 'x']          | _
        ['test1': 'y']          | _
        ['test1': 'x,y']        | _
        ['test1': 'some value'] | _
    }

    def "validate option values, enforced valid multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true, multivalued: true, delimiter: ',')
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts                         | _
        ['test1': 'a,b']             | _
        ['test1': ['a', 'b']]        | _
        ['test1': 'b,']              | _
        ['test1': 'abc,a,b']         | _
        ['test1': ['abc', 'a', 'b']] | _
    }

    def "validate option values, enforced invalid multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true, multivalued: true, delimiter: ',')
        option.addToValues('a')
        option.addToValues('b')
        option.addToValues('abc')
        se.addToOptions(option)

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == 'domain.Option.validation.allowed.values'

        where:
        opts                            | _
        ['test1': 'blah']               | _
        ['test1': 'a,blah']             | _
        ['test1': ['a', 'blah']]        | _
        ['test1': 'blah,']              | _
        ['test1': 'abc,a,blah']         | _
        ['test1': ['abc', 'a', 'blah']] | _
    }

    def "validate option values, enforced valid multivalue regex"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true, multivalued: true, delimiter: ' ', regex: '^[abc]+$')
        se.addToOptions(option)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        validation

        where:
        opts                           | _
        ['test1': 'abc']               | _
        ['test1': 'abc abccaba']       | _
        ['test1': ['abc']]             | _
        ['test1': ['abc', 'abcaccab']] | _
    }

    def "validate option values, enforced invalid multivalue regex"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: false, multivalued: true, delimiter: ' ', regex: '^[abc]+$')
        option.delimiter = ' '
        se.addToOptions(option)

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == 'domain.Option.validation.regex.values'

        where:
        opts                              | _
        ['test1': 'abcd']                 | _
        ['test1': 'abc abccabazzz']       | _
        ['test1': ['abczz']]              | _
        ['test1': ['abc', 'abcaccabzzz']] | _
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
        opts                    | _
        ['test1': 'x']          | _
        ['test1': 'y']          | _
        ['test1': 'x,y']        | _
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

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == 'domain.Option.validation.required'


        where:
        opts           | missingkey
        ['test2': 'a'] | 'test1'
        ['test1': 'a'] | 'test2'
    }

    def "validate option values, file type required"() {
        given:
        ScheduledExecution se = new ScheduledExecution(uuid: 'asdf')
        final Option option = new Option(name: 'test1', required: true, optionType: 'file')
        se.addToOptions(option)

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        service.fileUploadService = Mock(FileUploadService) {
            0 * validateFileRefForJobOption('aref', 'asdf', 'test1') >> [
                    valid: false
            ]
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == message


        where:
        opts           | message
        ['test2': 'a'] | 'domain.Option.validation.required'
    }

    def "validate option values, file type not valid"() {
        given:
        ScheduledExecution se = new ScheduledExecution(uuid: 'asdf')
        final Option option = new Option(name: 'test1', required: true, optionType: 'file')
        se.addToOptions(option)

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _, _) >> {
                it[0]
            }
        }
        service.fileUploadService = Mock(FileUploadService) {
            1 * validateFileRefForJobOption('aref', 'asdf', 'test1') >> [
                    valid: false, error: ecode, args: []
            ]
        }
        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        ExecutionServiceException e = thrown()
        e.message == message


        where:
        opts              | ecode       | message
        ['test1': 'aref'] | 'fileerror' | 'domain.Option.validation.file.fileerror'
        ['test1': 'aref'] | 'invalid'   | 'domain.Option.validation.file.invalid'
    }

    def "filter opts params string"() {
        given:
        def params = [
                'option.opt1': 'abc',
                'option.opt2': 'def'
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        'abc' == result.opt1
        'def' == result.opt2

    }

    def "filter opts params list"() {
        given:
        def params = [
                'option.opt1': ['abc', ''],
                'option.opt2': (['def', 'ghi'] as Set)
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        ['abc'] == result.opt1
        ['def', 'ghi'] == result.opt2

    }

    def "filter opts params string array"() {
        String[] strings = ['abc', '']
        String[] strings2 = ['def', 'ghi']
        given:
        def params = [
                'option.opt1': strings,
                'option.opt2': strings2
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        ['abc'] == result.opt1
        ['def', 'ghi'] == result.opt2

    }

    def "filter opts params incorrect type"() {
        given:
        def params = [
                'option.opt1': 123,
                'option.opt2': new Object(),
        ]
        when:
        def result = ExecutionService.filterOptParams(params)

        then:
        0 == result.size()
        null == result.opt1
        null == result.opt2

    }

    @Unroll
    def "parse job opts from string multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'opt1', enforced: false, multivalued: true, delimiter: ','))
        final opt2 = new Option(name: 'opt2', enforced: true, multivalued: true, delimiter: ' ')
        opt2.delimiter = ' '
        opt2.addToValues('a')
        opt2.addToValues('b')
        opt2.addToValues('abc')
        se.addToOptions(opt2)


        when:
        def result = service.parseJobOptsFromString(se, argString)

        then:
        result == expected

        where:
        argString                | expected
        '-opt1 test'             | [opt1: ['test']]
        '-opt1 test,x'           | [opt1: ['test', 'x']]
        '-opt1 \'test x\''       | [opt1: ['test x']]
        '-opt2 a'                | [opt2: ['a']]
        '-opt2 a,b'              | [opt2: ['a,b']]
        '-opt2 \'blah zah nah\'' | [opt2: ['blah', 'zah', 'nah']]


    }

    def "can read storage password"() {
        given:
        AuthContext context = Mock(AuthContext)
        service.storageService = Mock(StorageService)

        when:
        def result = service.canReadStoragePassword(context, path, false)

        then:
        service.storageService.storageTreeWithContext(context) >> Mock(KeyStorageTree) {
            1 * readPassword(path) >> {
                if (throwsexception) {
                    throw new StorageException(StorageException.Event.READ, PathUtil.asPath(path))
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

    def "list now running for project includes scheduled"() {
        given:
        def query = new QueueQuery()
        query.projFilter = 'AProject'
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 45)
        def exec = new Execution(
                dateStarted: cal.getTime(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        when:
        def result = service.queryQueue(query)

        then:
        'scheduled' == exec.getExecutionState()
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

    @Unroll
    def "should scheduleAdHocJob with runAtTime"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        Date scheduleDate = new Date().copyWith(
                year: 2080,
                month: Calendar.JULY,
                dayOfMonth: 5,
                hourOfDay: 16,
                minute: 05,
                second: 45
        )
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        Map params = [runAtTime: "2080-07-05T16:05:45.000+0000"]

        when:
        def result = service.scheduleAdHocJob(job, authContext, "user1", params)

        then:
        1 * service.scheduledExecutionService.scheduleAdHocJob(*_) >> { args ->
            final Date startDate    = args[7]
            // The start time may differ slightly (milliseconds)
            assert startDate.getTime() - scheduleDate.getTime() <= 500 ||
                startDate.getTime() - scheduleDate.getTime() >= -500
            return scheduleDate
        }
        result.nextRun.getTime() == scheduleDate.getTime()

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Unroll
    def "should not scheduleAdHocJob if no date/time"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()

        when:
        def result = service.scheduleAdHocJob(job, authContext, "user1", [:])

        then:
        result.success == false
        result.error == "failed"
        result.message == "A date and time is required to schedule a job"
        result.failed == true

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Unroll
    def "should not scheduleAdHocJob with time in past"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        def Map params  = [runAtTime: "1999-01-01T01:02:42.000+0000"]

        when:
        def result = service.scheduleAdHocJob(job, authContext, "user1", params)

        then:
        result.success == false
        result.error == "failed"
        result.message == "A job cannot be scheduled for a time in the past"
        result.failed == true

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Unroll
    def "should scheduleAdHocJob with alternative ISO 8601 date"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        Date scheduleDate = new Date().copyWith(
                year: 2200,
                month: Calendar.JANUARY,
                dayOfMonth: 1,
                hourOfDay: 12,
                minute: 43,
                second: 10
        )
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        def Map params  = [runAtTime: runAtTime]

        when:
        def result = service.scheduleAdHocJob(job, authContext, "user1", params)

        then:
        1 * service.scheduledExecutionService.scheduleAdHocJob(*_) >> { args ->
            final Date startDate    = args[7]
            // The start time may differ slightly (milliseconds)
            assert startDate.getTime() - scheduleDate.getTime() <= 500 ||
                startDate.getTime() - scheduleDate.getTime() >= -500
            return scheduleDate
        }
        result.nextRun.getTime() == scheduleDate.getTime()

        where:
        runAtTime                       | executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        "2200-01-01T12:43:10.000+00:00" | true                | true            | true             | true        | true
        "2200-01-01T12:43:10.000Z"      | true                | true            | true             | true        | true
        "2200-01-01T12:43:10+00:00"     | true                | true            | true             | true        | true
        "2200-01-01T12:43:10Z"          | true                | true            | true             | true        | true
    }

    @Unroll
    def "should not scheduleAdHocJob with invalid ISO 8601 date"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
            authorizeProjectJobAll(*_) >> true
        }
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        def Map params  = [runAtTime: time]

        when:
        def result = service.scheduleAdHocJob(job, authContext, "user1", params)

        then:
        result.success == false
        result.error == "failed"
        result.message ==~ /^Invalid date.*$/
        result.failed == true

        where:
        time                               | executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        "01/01/2001 10:11:12.000000 +0000" | true                | true            | true             | true        | true
        "0000-00-00 00:00:00.000+0000"     | true                | true            | true             | true        | true
        "2080-01-01T01:00:01.000"          | true                | true            | true             | true        | true
    }

    @Unroll
    def "abort execution"() {
        given:
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.metricService = Mock(MetricService)
        service.frameworkService = Mock(FrameworkService)
        service.reportService = Mock(ReportService)
        service.notificationService = Mock(NotificationService)
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: false,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        ).save()

        def e = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject',
                status: isadhocschedule ? 'scheduled' : null,
                serverNodeUUID: (cmatch ? null : UUID.randomUUID().toString()),
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ).save(),
                ).save(flush: true)
        def user = 'userB'
        def auth = Mock(AuthContext)

        when:
        def result = service.abortExecution(job, e, user, auth)

        then:
        e.id != null
        result.abortstate == eAbortstate
        result.jobstate == eJobstate

        1 * service.scheduledExecutionService.getJobIdent(job, e) >> [jobname: 'test', groupname: 'testgroup']
        1 * service.frameworkService.authorizeProjectExecutionAll(auth, e, [AuthConstants.ACTION_KILL]) >> true
        1 * service.frameworkService.isClusterModeEnabled() >> iscluster
        if(cmatch) {
            1 * service.scheduledExecutionService.findExecutingQuartzJob(job, e) >>
                    (wasScheduledPreviously ? 'unique-id' : null)
            1 * service.scheduledExecutionService.interruptJob(_, 'test', 'testgroup', isadhocschedule) >>
                    didinterrupt
            _ * service.reportService.reportExecutionResult(_) >> [:]
        }


        where:
        isadhocschedule | wasScheduledPreviously | didinterrupt | iscluster | cmatch | eAbortstate | eJobstate
        true            | true                   | true         | false     | true   | 'pending'   | 'running'
        false           | true                   | true         | false     | true   | 'pending'   | 'running'
        true            | false                  | true         | false     | true   | 'aborted'   | 'aborted'
        false           | false                  | true         | false     | true   | 'aborted'   | 'aborted'
        true            | true                   | false        | false     | true   | 'failed'    | 'running'
        false           | true                   | false        | false     | true   | 'failed'    | 'running'
        true            | false                  | false        | false     | true   | 'aborted'   | 'aborted'
        false           | false                  | false        | false     | true   | 'aborted'   | 'aborted'
        true            | true                   | true         | true      | true   | 'pending'   | 'running'
        true            | true                   | true         | true      | false  | 'failed'    | 'running'

    }
}

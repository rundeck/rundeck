package rundeck

import com.dtolabs.rundeck.app.internal.logging.LogFlusher

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
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.execution.WorkflowExecutionListenerTest
import groovy.time.TimeCategory
import org.rundeck.app.auth.types.AuthorizingProject
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.authorization.domain.execution.AuthorizingExecution
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequestImpl
import org.rundeck.app.data.providers.GormExecReportDataProvider
import org.rundeck.app.data.providers.GormReferencedExecutionDataProvider
import org.rundeck.app.data.providers.GormJobStatsDataProvider
import org.rundeck.app.data.providers.GormUserDataProvider
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.SelectorUtils
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.execution.ExecutionItemFactory
import com.dtolabs.rundeck.core.jobs.JobRefCommand
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.spring.AutowiredTest
import org.grails.events.bus.SynchronousEventBus
import org.grails.plugins.metricsweb.MetricService
import org.grails.web.json.JSONObject
import org.rundeck.app.services.ExecutionFile
import org.rundeck.core.auth.access.UnauthorizedAccess
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import org.springframework.context.MessageSource
import rundeck.services.*
import rundeck.services.data.UserDataService
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Specification
import spock.lang.Unroll

import java.sql.Time
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Created by greg on 2/17/15.
 */
class ExecutionServiceSpec extends Specification implements ServiceUnitTest<ExecutionService>, DataTest, AutowiredTest {

    Class[] getDomainClassesToMock() {
        [Execution, User, ScheduledExecution, Workflow, CommandExec, Option, ExecReport, LogFileStorageRequest, ReferencedExecution, ScheduledExecutionStats, Notification]
    }

    def setup(){
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService)
        service.executionValidatorService = new ExecutionValidatorService()
        service.logFileStorageService=Mock(LogFileStorageService)
        service.fileUploadService=Mock(FileUploadService)

        mockDataService(UserDataService)
        GormUserDataProvider provider = new GormUserDataProvider()
        GormExecReportDataProvider providerExec = new GormExecReportDataProvider()
        GormReferencedExecutionDataProvider referencedExecutionDataProvider = new GormReferencedExecutionDataProvider()
        service.userDataProvider = provider
        service.execReportDataProvider = providerExec
        service.referencedExecutionDataProvider = referencedExecutionDataProvider
        service.jobStatsDataProvider = new GormJobStatsDataProvider()
    }

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
                uuid: UUID.randomUUID().toString(),
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
        e.message ==~ /.*running executions has been reached.*/
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
                project: 'AProject',
                executionType: 'scheduled'
        ).save()
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(job, authContext, null, ['extra.option.test': '12',executionType: 'scheduled'], true, exec.id)

        then:
        e2 != null
        e2.executionType == 'scheduled'
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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['extra.option.test': '12', executionType: 'user']
        )

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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                null,
                [executionType: 'user']
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
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'Temp/adHoc',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a Adhoc command job',
                argString: argString,
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )

        def params = [
                project    : 'AProject',
                groupPath  : 'some/where',
                description: 'a job',
                argString  : argString,
                user       : 'bob',
                workflow   : scheduledExecution.workflow,
                retry      : '1'
        ]

        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }

        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }

        when:
        Execution e2 = service.createExecutionAndPrep(
                scheduledExecution,
                authContext,
                params
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
        }
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAll(*_) >> true
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            isProjectExecutionEnabled(_) >> true
        }
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> true
        }

        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        def result = service.executeJob(job, authContext, 'test2', [executionType: 'user'])

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

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
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
            final Date startDate    = args[6]
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
                SaveReportRequestImpl saveReportRequest = args[0]
                params = [
                        'executionId': saveReportRequest.executionId,
                        'jcJobId': saveReportRequest.jobId,
                        'reportId': saveReportRequest.reportId,
                        'adhocExecution': saveReportRequest.adhocExecution,
                        'ctxProject': saveReportRequest.project,
                        'author': saveReportRequest.author,
                        'title': saveReportRequest.title,
                        'status': saveReportRequest.status,
                        'node': saveReportRequest.node,
                        'message': saveReportRequest.message,
                        'dateStarted': saveReportRequest.dateStarted,
                        'dateCompleted': saveReportRequest.dateCompleted,
                        'succeededNodeList': saveReportRequest.succeededNodeList,
                        'failedNodeList': saveReportRequest.failedNodeList,
                        'filterApplied': saveReportRequest.filterApplied,
                ]
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
        params.executionId == 11
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
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * filterAuthorizedNodes(*_)
        }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test1', '${option.test1}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, null, false, false, true, false
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

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_) >> ['a': 'b', c: 'd']
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],null,false,null,null,false,null,false,false,true, false
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


            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                exec,
                context,
                args as String[],
                null, null, null, null, null, null, false, false, true, false
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
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],//null values for the input options
                null, null, null, null, null, null, false, false, true, false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': 'newtest2']
        newCtxt.dataContext['option'] == ['test2': 'newtest2']
        newCtxt.privateDataContext['option'] == ['test1': 'newtest1']

        service.storageService.storageTreeWithContext(_) >> Mock(KeyStorageTree) {
            1 * hasPassword('keys/test1') >> true
            1 * readPassword('keys/test1') >> {
                return 'newtest1'.bytes
            }
            1 * hasPassword('keys/test2') >> true
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
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test1', '${option.zilch}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null, null, false, false, true, false
        )

        then:

        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo']
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']
    }

    @Unroll
    def "createJobReferenceContext shared variable expansion in args with node? #nodename"() {
        given:
        def sharedContext = SharedDataContextUtils.sharedContext()
        sharedContext.merge(ContextView.global(), DataContextUtils.context("rarity", [globular: "globalvalue"]))
        sharedContext.merge(ContextView.node('anode'), DataContextUtils.context("rarity", [globular: "anodevalue"]))
        def context = ExecutionContextImpl
                .builder()
                .threadCount(1)
                .keepgoing(false)

                .dataContext(
                ['option': ['monkey': 'wakeful'], 'secureOption': ['test2': 'zimbo'], 'job': ['execid': '123']]
        )
                .privateDataContext(['option': ['zilch': 'phoenix'],])
                .mergeSharedContext(sharedContext)
                .user('aUser')
                .build()
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
        def opt3 = new Option(name: 'test3', enforced: false, required: false, secureInput: false)
        assert opt1.validate()
        assert opt2.validate()
        assert opt3.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        se.addToOptions(opt3)
        null != se.save()

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * filterAuthorizedNodes(*_)
        }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        def contextNode = nodename ? new NodeEntryImpl(nodename) : null

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test3', '${rarity.globular}', '-test1', '${option.zilch}', '-test2', '${option.test2}'] as String[],
                null, null, null, null, null,
                contextNode,
                false, false, true, false
        )

        then:
        sharedContext.resolve(ContextView.global(), 'rarity', 'globular') == 'globalvalue'
        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo', 'test3': expect]
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']

        where:
        nodename | expect
        null     | 'globalvalue'
        'anode'  | 'anodevalue'
        'bnode'  | 'globalvalue'
    }

    @Unroll
    def "overrideJobReferenceNodeFilter uses shared variable expansion in #nodeFilter"() {
        given:
        def sharedContext = SharedDataContextUtils.sharedContext()
        sharedContext.merge(ContextView.global(), DataContextUtils.context("shared", [nodea: "b"]))
        sharedContext.merge(ContextView.global(), DataContextUtils.context("global", [nodea: "a"]))
        sharedContext.merge(ContextView.node('anode'), DataContextUtils.context("shared", [nodea: "c"]))
        sharedContext.merge(ContextView.node('anode'), DataContextUtils.context("nodecontext", [nodea: "a"]))

        def makeNodeSet = { list ->
            def nodeset = new NodeSetImpl()
            list.each {
                nodeset.putNode(new NodeEntryImpl(it))
            }
            nodeset
        }
        def allNodes = makeNodeSet(['a', 'b', 'c', 'x', 'y', 'z'])
        def context = ExecutionContextImpl.builder()
                                          .nodes(allNodes)
                                          .nodeSelector(SelectorUtils.nodeList(['a', 'b', 'c', 'x', 'y', 'z']))
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(DataContextUtils.context('data', [nodea: 'z']))
                                          .mergeSharedContext(sharedContext)
                                          .build()

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(_, _, _, _) >> { args ->
                    args[2]
                }
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(_, _) >> { args ->
                com.dtolabs.rundeck.core.common.NodeFilter.filterNodes(args[0], allNodes)
            }
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)


        when:

        def newCtxt = service.overrideJobReferenceNodeFilter(
                null,
                context,
                ExecutionContextImpl.builder().build(),
                nodeFilter,
                2,
                true,
                null,
                null,
                false
        )

        then:
        newCtxt.nodes.nodeNames == expect as Set

        where:
        nodeFilter                       | expect
        'x y'                            | ['x', 'y']
        '${bad.wrong} x y'               | ['x', 'y']
        '${data.nodea} x y'              | ['z', 'x', 'y']
        '${global.nodea} x y'            | ['a', 'x', 'y']
        '${shared.nodea} x y'            | ['b', 'x', 'y']
        '${shared.nodea@anode} x y'      | ['c', 'x', 'y']
        '${nodecontext.nodea} x y'       | ['x', 'y']
        '${nodecontext.nodea@anode} x y' | ['a', 'x', 'y']
    }


    def "Create execution context with global vars"() {
        given:

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
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

    def "Create execution context with email of user profile"() {
        given:

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * getProjectGlobals(*_) >> [:]
            0 * _(*_)
        }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }

        User user = new User(login: 'testuser', password: '12345', email: 'email@test.com')
        user.save(flush: true)

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false
        )

        when:
        def val = service.createContext(se, null, null, null, null, [:], null, null, null, null, null, null)
        then:
        val != null
        val.dataContext.job['user.email'] == user.email

        where:
        charset      | _
        null         | _
        'UTF-8'      | _
        'ISO-8859-1' | _
    }

    def "Create execution context with charset"() {
        given:

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
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
        def val = service.createContext(se, null, null, null, null, null, null, null, null, null, null, charset)
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
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        def auth = Mock(AuthContext)
        def execution = new Execution()

        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.rundeckAuthContextProcessor.authResourceForProject(_)
        1 * service.rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        ) >> false
        !result.success
        result.error == 'unauthorized'

    }

    def "delete execution authorizingProject unauthorized"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        def eauth = Mock(AuthorizingExecution)
        def pauth = Mock(AuthorizingProject)

        when:
        def result = service.deleteExecution(pauth,eauth)

        then:
        1 * pauth.authorize(RundeckAccess.Project.APP_DELETE_EXECUTION)>>{
            throw new UnauthorizedAccess('delete','execution','blah')
        }
        UnauthorizedAccess t = thrown()
    }
    def "delete execution authorizingProject ok"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        assert execution.save()
        ExecReport execReport = ExecReport.fromExec(execution)
        assert execReport!=null
        def erptid=execReport.id
        def eauth = Mock(AuthorizingExecution){
            getResource()>>execution
        }
        def pauth = Mock(AuthorizingProject)

        when:
        def result = service.deleteExecution(pauth,eauth)

        then:
        1 * pauth.authorize(RundeckAccess.Project.APP_DELETE_EXECUTION)
        1 * pauth.getAuthContext()>>Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
        }
        1 * service.logFileStorageService.getExecutionFiles(execution,_,_)>>[:]
        result.success
        ExecReport.get(erptid)==null
    }

    def "delete execution running"() {
        given:

        service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        def auth = Mock(AuthContext)
        def execution = new Execution()
        execution.dateStarted = new Date()

        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.rundeckAuthContextProcessor.authResourceForProject(_)
        1 * service.rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        ) >> true

        !result.success
        result.error == 'running'
    }

    def "delete execution files"() {
        given:

        service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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

        def file1 = File.createTempFile("rundeck.ExecutionServiceSpec-test", "file")
        file1.deleteOnExit()
        def file2 = File.createTempFile("rundeck.ExecutionServiceSpec-test", "file")
        file2.deleteOnExit()

        ExecutionFile executionFile1 = Mock(ExecutionFile){
            getLocalFile() >> file1
        }

        Map <String, ExecutionFile> executionFiles = ['rdlog': executionFile1,'state.json':  executionFile1]


        service.fileUploadService = Mock(FileUploadService)
        service.logFileStorageService = Mock(LogFileStorageService) {
            1 * getFileForExecutionFiletype(execution, 'rdlog', false, false) >> file1
            1 * getFileForExecutionFiletype(execution, 'rdlog', false, true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', false, false) >> file2
            1 * getFileForExecutionFiletype(execution, 'state.json', false, true) >> file2
            1 * getExecutionFiles(execution, [], false) >> executionFiles
            1 * removeRemoteLogFile(execution, 'rdlog') >> [started: false, error: "not found"]
            1 * removeRemoteLogFile(execution, 'state.json') >> [started: false, error: "not found"]
            0 * _(*_)
        }


        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.rundeckAuthContextProcessor.authResourceForProject(_)
        1 * service.rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        ) >> true

        result.success

        !file1.exists()
        !file2.exists()
    }

    def "delete execution files failure"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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


        ExecutionFile executionFile1 = Mock(ExecutionFile){
            getLocalFile() >> file1
        }

        Map <String, ExecutionFile> executionFiles = ['rdlog': executionFile1,'state.json':  executionFile1]

        service.fileUploadService = Mock(FileUploadService)
        service.logFileStorageService = Mock(LogFileStorageService) {
            1 * getFileForExecutionFiletype(execution, 'rdlog', false, false) >> file1
            1 * getFileForExecutionFiletype(execution, 'rdlog', false, true) >> file1
            1 * getFileForExecutionFiletype(execution, 'state.json', false, false)
            1 * getFileForExecutionFiletype(execution, 'state.json', false, true)
            1 * getExecutionFiles(execution, [], false) >> executionFiles
            1 * removeRemoteLogFile(execution, 'rdlog') >> [started: false, error: "not found"]
            1 * removeRemoteLogFile(execution, 'state.json') >> [started: false, error: "not found"]
            0 * _(*_)
        }


        when:
        def result = service.deleteExecution(execution, auth, 'bob')

        then:
        1 * service.rundeckAuthContextProcessor.authResourceForProject(_)
        1 * service.rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
        ) >> true

        result.success
    }

    def "delete execution job file records"() {
        given:

        service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * service.rundeckAuthContextProcessor.authResourceForProject(_)
        1 * service.rundeckAuthContextProcessor.authorizeApplicationResourceAny(
                auth,
                _,
                [AuthConstants.ACTION_DELETE_EXECUTION, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN]
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
            hasPassword('keys/opt1') >> {
                if (hasopt1) {
                    return true
                }else{
                    return false
                }
            }
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
            hasPassword('keys/opt2') >> {
                if (hasopt2) {
                    return true
                }else{
                    return false
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

    def "loadSecureOptionStorageDefaults failed required"() {
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
                                defaultStoragePath: 'keys/opt1',
                                required: true
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
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext, true)

        then:
        1 * service.storageService.storageTreeWithContext(authContext) >> Mock(KeyStorageTree) {
            readPassword('keys/opt1') >> {
                if (!readerr) {
                    return 'newopt1'.bytes
                }
                if (readerr) {
                    throw new StorageException(
                            "not found",
                            StorageException.Event.READ,
                            PathUtil.asPath('keys/opt1')
                    )
                }
            }

        }
        opt1result == secureOpts['opt1']
        ExecutionServiceException e = thrown()
        e.message.contains(expecterr)

        where:
        inputExposed | inputSecure | opt1result | readerr | expecterr
        [:]          | [:]         | null       | true    | 'not found'

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
                hasPassword('keys/opt1') >> true
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

    def "invalid option values, opt enforced allowed values from Remote Url"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: 'test1', enforced: true, optionValues: null)
        se.addToOptions(opt)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:

        service.validateOptionValues(se, opts)

        then:
        1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_,_) >> {
            [
                    optionSelect : opt,
                    values       : remoteValues,
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }


        ExecutionServiceValidationException e = thrown()
        e.errors.containsKey('test1')


        where:
        opts                                           | remoteValues
        ['test1': 'somevalue']                         | ["A", "B", "C"]
        ['test1': 'somevalue']                         | [new JSONObject(name: "a", value:"A"), new JSONObject(name:"b", value:"B"), new JSONObject(name:"c", value:"C")]
        ['test1': 'somevalue']                         | [[name: 'bar', value:'Bar']]
    }

    def "valid option values, opt enforced allowed values from Remote Url"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: 'test1', enforced: true, optionValues: null)
        se.addToOptions(opt)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:

        service.validateOptionValues(se, opts)

        then:
        1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_,_) >> {
            [
                    optionSelect : opt,
                    values       : remoteValues,
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }

        noExceptionThrown()


        where:
        opts                                           | remoteValues
        ['test1': 'Foo']                               | ["Foo", "Bar"]
        ['test1': 'A']                                 | [new JSONObject(name: "a", value:"A"), new JSONObject(name:"b", value:"B"), new JSONObject(name:"c", value:"C")]
        ['test1': 'Bar']                               | [[name: 'bar', value:'Bar']]
    }

    def "remote url option validator does not attempt to validate option values plugin values"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: 'test1', enforced: true, optionValues: null, optionValuesPluginType: 'test_opt_val_plugin')
        se.addToOptions(opt)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:

        def validation = service.validateOptionValues(se, ["test":"A"])

        then:
        0 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_)
        noExceptionThrown()

    }

    def "opt enforced allowed values from Remote Url with or without default value"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: 'test1', enforced: true, defaultValue: defaultValue, optionValues: null)
        se.addToOptions(opt)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_,_) >> {
            [
                    optionSelect : opt,
                    values       : remoteValues,
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }
        when:

        HashMap optparams = service.parseJobOptionInput([:], se, null)

        then:


        optparams[opt.name] == optValue


        where:
        defaultValue | optValue                                       | remoteValues
        "B"          | "B"                                            | [new JSONObject(name: "a", value:"A", selected: true), new JSONObject(name:"b", value:"B"), new JSONObject(name:"c", value:"C")]
        null         | "A"                                            | [new JSONObject(name: "a", value:"A", selected: true), new JSONObject(name:"b", value:"B"), new JSONObject(name:"c", value:"C")]
        null         | null                                           | ['A','B','C']

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
                hasPassword('keys/opt1') >> false
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

    def "validate option values, regex failure but success for multivalued option"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'test3', enforced: false, regex: 'shampoo[abc].*'))
        se.addToOptions(new Option(name: 'test4', enforced: false, multivalued: true, regex: 'shampoo[abc].*'))

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
        opts                                                     | _
        ['test3': 'shampooz', 'test4': ['shampooa', 'shampoob']] | _
    }

    def "validate option values, enforced valid"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        final Option option = new Option(name: 'test1', enforced: true)
        option.valuesList = 'a,b,abc'
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
        option.valuesList = 'a,b,abc'
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
        option.valuesList = 'a,b,abc'
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
        option.valuesList = 'a,b,abc'
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

        service.scheduledExecutionService = Mock(ScheduledExecutionService)

        when:

        def validation = service.validateOptionValues(se, opts)

        then:
        1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_,_) >> {
            [
                    optionSelect : option,
                    values       : [],
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }

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
        option.valuesList = 'a,b,abc'
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
        option.valuesList = 'a,b,abc'
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
            1 * validateFileRefForJobOption('aref', 'asdf', 'test1', false) >> [
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

    def "can read storage password"() {
        given:
        AuthContext context = Mock(AuthContext)
        service.storageService = Mock(StorageService)

        when:
        def result = service.canReadStoragePassword(context, path, false)

        then:
        service.storageService.storageTreeWithContext(context) >> Mock(KeyStorageTree) {
            1 * hasPassword(path) >> {
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
        service.configurationService = Mock(ConfigurationService)
        when:
        def result = service.queryQueue(query)

        then:
        1 == result.total
        1 == result.nowrunning.size()

    }

    @Unroll
    def "list should not consider postponed runs as running filter: considerPostpone: #considerPostponedRunsAsRunningFilter, nowRunning: #nowrunning"() {
        given:
        def query = new QueueQuery()
        Date now = new Date()
        Date future = now
        use(TimeCategory) {
            future = future + 1.days
            now = now - 1.hours
        }
        query.projFilter = 'AProject'
        query.runningFilter = 'running'
        query.considerPostponedRunsAsRunningFilter = considerPostponedRunsAsRunningFilter
        def exec = new Execution(
                dateStarted: now,
                dateCompleted: null,
                user: 'userB',
                project: 'AProject',
                status: status
        ).save()
        def exec2 = new Execution(
                dateStarted: future,
                dateCompleted: null,
                user: 'user',
                project: 'AProject',
                status: 'scheduled'
        ).save()
        def exec3 = new Execution(
                dateStarted: now,
                dateCompleted: null,
                user: 'user',
                project: 'AProject',
                status: 'queued'
        ).save()
        service.configurationService = Mock(ConfigurationService)

        when:
        def result = service.queryQueue(query)

        then:
        nowrunning == result.total
        nowrunning == result.nowrunning.size()

        where:
        considerPostponedRunsAsRunningFilter | nowrunning | status
        true                                 | 3          | null
        true                                 | 3          | 'scheduled'
        true                                 | 3          | 'queued'
        false                                | 1          | null
        false                                | 0          | 'scheduled'
        false                                | 0          | 'queued'
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
        service.configurationService = Mock(ConfigurationService)

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
        service.configurationService = Mock(ConfigurationService)

        when:
        def result = service.queryQueue(query)

        then:
        2 == result.total
        2 == result.nowrunning.size()

    }

    def "list now running multiple projects"() {
        given:
            def query = new QueueQuery()
            query.projFilter = 'AProject,BProject'
            def exec = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: 'userB',
                    project: 'AProject'
            ).save(flush: true)
            def exec2 = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: 'user',
                    project: 'BProject'
            ).save(flush: true)
            def exec3 = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: 'user',
                    project: 'CProject'
            ).save(flush: true)
            service.configurationService = Mock(ConfigurationService)

        when:
            def result = service.queryQueue(query)

        then:
            exec
            exec2
            exec3
            2 == result.total
            2 == result.nowrunning.size()

    }

    @Unroll
    def "should scheduleAdHocJob with runAtTime"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAll(*_) >> true
        }

        Date scheduleDate = createDate(2080, Calendar.JULY, 5, 16, 05, 45, 'Z')
        service.configurationService = Stub(ConfigurationService) {
            isExecutionModeActive() >> executionsAreActive
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
            getRoles() >> (['c', 'd'] as Set)
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
            final Date startDate    = args[6]
            // The start time may differ slightly (milliseconds)
            assert startDate.getTime() - scheduleDate.getTime() <= 500 ||
                startDate.getTime() - scheduleDate.getTime() >= -500
            return scheduleDate
        }
        result.nextRun.getTime() == scheduleDate.getTime()
        result.execution != null
        result.execution.user == 'user1'
        result.execution.userRoles == ['c', 'd']


        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Unroll
    def "should not scheduleAdHocJob if no date/time"() {
        given:
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
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
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
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
        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
            }
        Date scheduleDate = createDate(2200, Calendar.JANUARY, 1, 12, 43, 10, 'Z')

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
            final Date startDate    = args[6]
            // The start time may differ slightly (milliseconds)
            assert startDate.getTime() - scheduleDate.getTime() <= 1000 &&
                startDate.getTime() - scheduleDate.getTime() >= -1000
            return scheduleDate
        }
        result.nextRun.getTime() == scheduleDate.getTime()

        where:
        runAtTime                       | executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        "2200-01-01T12:43:10.000+00:00" | true                | true            | true             | true        | true
        "2200-01-01T12:43:10.000Z"      | true                | true            | true             | true        | true
        "2200-01-01T12:43:10+00:00"     | true                | true            | true             | true        | true
        "2200-01-01T12:43:10Z"          | true                | true            | true             | true        | true
        "2200-01-01T18:13:10+05:30"     | true                | true            | true             | true        | true
        "2200-01-01T18:13:10.000+05:30" | true                | true            | true             | true        | true
        "2200-01-01T09:13:10-03:30"     | true                | true            | true             | true        | true
        "2200-01-01T09:13:10.000-03:30" | true                | true            | true             | true        | true
    }

    private Date createDate(
            int year,
            int month,
            int dayOfMonth,
            int hour,
            int minute,
            int second,
            String zoneId = 'Z'
    )
    {
        def cal = GregorianCalendar.getInstance(TimeZone.getTimeZone(ZoneId.of(zoneId)))
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, second)

        Date.from(cal.toInstant())
    }

    @Unroll
    def "parseRunAt alternative ISO 8601 date"() {
        given:

        Date expectedDate = new Date(7258164190000)

        when:
        def parsedDate = service.parseRunAtTime(runAtTime)

        then:
        // The start time may differ slightly (milliseconds)
        assert parsedDate.time - expectedDate.time <= 1000 && parsedDate.time - expectedDate.time >= -1000

        where:
        runAtTime                       | _
        "2200-01-01T12:43:10.000+00:00" | _
        "2200-01-01T12:43:10.000Z"      | _
        "2200-01-01T12:43:10+00:00"     | _
        "2200-01-01T12:43:10Z"          | _
        "2200-01-01T18:13:10+05:30"     | _
        "2200-01-01T18:13:10.000+05:30" | _
        "2200-01-01T09:13:10-03:30"     | _
        "2200-01-01T09:13:10.000-03:30" | _
    }

    @Unroll
    def "should not scheduleAdHocJob with invalid ISO 8601 date"() {
        given:

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
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
        "2200-01-01 18:13:10.000 +05:30"   | true                | true            | true             | true        | true
        "2080-01-01 01:00:01.000 -03:30"   | true                | true            | true             | true        | true
    }

    @Unroll
    def "abort execution"() {
        given:
        def serverUuid='541bf763-39a6-44ff-8c68-c9d53f6eec33'
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.metricService = Mock(MetricService)
        service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        service.reportService = Mock(ReportService)
        service.notificationService = Mock(NotificationService)
        service.workflowService = Mock(WorkflowService)
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
                serverNodeUUID: (cmatch ? null : serverUuid),
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ).save(),
                ).save(flush: true)
        def user = 'userB'
        def auth = Mock(AuthContext)
        service.configurationService = Mock(ConfigurationService) {
            getString('executionService.startup.cleanupStatus', _) >> 'incompletestatus'
        }
        if(!cmatch) {
            def seventBus = new SynchronousEventBus()
            service.setTargetEventBus(seventBus)
            seventBus.subscribe("cluster.abortExecution") { eventData ->
                println "received event data: ${eventData}"
                return [:]
            }
        }

        when:
        def result = service.abortExecution(job, e, user, auth, asuser, forced)

        then:
        Execution.withSession {session->
            session.flush()
            e.refresh()
        }
        e.id != null
        result.abortstate == eAbortstate
        result.jobstate == eJobstate
        result.reason == reason
        e.status == (isadhocschedule&&wasScheduledPreviously?'scheduled':estatus)
        e.cancelled == ecancelled
        e.abortedby==(cmatch?(asuser?:'userB'):null)

        1 * service.scheduledExecutionService.getJobIdent(job, e) >> [jobname: 'test', groupname: 'testgroup']
        1 * service.rundeckAuthContextProcessor.authorizeProjectExecutionAll(auth, e, [AuthConstants.ACTION_KILL]) >> true
        if(asuser) {
            1 * service.rundeckAuthContextProcessor.authorizeProjectExecutionAll(auth, e, [AuthConstants.ACTION_KILLAS]) >> true
        }
        1 * service.frameworkService.isClusterModeEnabled() >> iscluster
        if(cmatch) {
            1 * service.scheduledExecutionService.findExecutingQuartzJob(job, e) >>
                    (wasScheduledPreviously ? 'unique-id' : null)
            1 * service.scheduledExecutionService.interruptJob(_, 'test', 'testgroup', isadhocschedule) >>
                    didinterrupt
        }
        _ * service.reportService.reportExecutionResult(_) >> [:]


        where:
        isadhocschedule | wasScheduledPreviously | didinterrupt | iscluster | cmatch | forced | eAbortstate | eJobstate| estatus            | ecancelled | asuser  |reason
        true            | true                   | true         | false     | true   | false  | 'pending'   | 'running'| 'false'            | false      | null    | null
        true            | true                   | true         | false     | true   | false  | 'pending'   | 'running'| 'false'            | false      | 'userC' | null
        false           | true                   | true         | false     | true   | false  | 'pending'   | 'running'| null               | false      | null    | null
        false           | true                   | true         | false     | true   | false  | 'pending'   | 'running'| null               | false      | 'userC' | null
        true            | false                  | true         | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | null    | null
        true            | false                  | true         | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | 'userC' | null
        false           | false                  | true         | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | null    | null
        false           | false                  | true         | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | 'userC' | null
        true            | true                   | false        | false     | true   | false  | 'failed'    | 'running'| 'false'            | false      | null    | 'Unable to interrupt the running job'
        true            | true                   | false        | false     | true   | false  | 'failed'    | 'running'| 'false'            | false      | 'userC' | 'Unable to interrupt the running job'
        false           | true                   | false        | false     | true   | false  | 'failed'    | 'running'| null               | false      | null    | 'Unable to interrupt the running job'
        false           | true                   | false        | false     | true   | false  | 'failed'    | 'running'| null               | false      | 'userC' | 'Unable to interrupt the running job'
        true            | false                  | false        | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | null    | null
        true            | false                  | false        | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | 'userC' | null
        false           | false                  | false        | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | null    | null
        false           | false                  | false        | false     | true   | false  | 'aborted'   | 'aborted'| 'false'            | true       | 'userC' | null
        true            | true                   | true         | true      | true   | false  | 'pending'   | 'running'| 'false'            | false      | null    | null
        true            | true                   | true         | true      | true   | false  | 'pending'   | 'running'| 'false'            | false      | 'userC' | null
        true            | true                   | true         | true      | false  | false  | 'failed'    | 'running'| 'false'            | false      | null    | 'Execution is running on a different cluster server: 541bf763-39a6-44ff-8c68-c9d53f6eec33'
        true            | true                   | true         | true      | false  | false  | 'failed'    | 'running'| 'false'            | false      | 'userC' | 'Execution is running on a different cluster server: 541bf763-39a6-44ff-8c68-c9d53f6eec33'
        false           | true                   | false        | false     | true   | true   | 'aborted'   | 'aborted'| 'incompletestatus' | false      | null    | 'Marked as incompletestatus'
        false           | true                   | false        | false     | true   | true   | 'aborted'   | 'aborted'| 'incompletestatus' | false      | 'userC' | 'Marked as incompletestatus'
        false           | false                  | false        | false     | true   | true   | 'aborted'   | 'aborted'| 'incompletestatus' | false      | null    | 'Marked as incompletestatus'
        false           | false                  | false        | false     | true   | true   | 'aborted'   | 'aborted'| 'incompletestatus' | false      | 'userC' | 'Marked as incompletestatus'

    }

    @Unroll
    def "abort execution authorizing check fail"() {
        given:
            service.scheduledExecutionService = Mock(ScheduledExecutionService)
            service.metricService = Mock(MetricService)
            service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            service.reportService = Mock(ReportService)
            service.notificationService = Mock(NotificationService)
            service.workflowService = Mock(WorkflowService)
            def authorizingExec = Mock(AuthorizingExecution){
                1 * access(RundeckAccess.Execution.APP_KILL)>>{
                    throw new UnauthorizedAccess('kill','execution','blah')
                }
            }
        when:
            def result = service.abortExecution(authorizingExec, null, false)
        then:
            UnauthorizedAccess t = thrown()
    }
    @Unroll
    def "abort execution as user authorizing check fail"() {
        given:
            service.scheduledExecutionService = Mock(ScheduledExecutionService)
            service.metricService = Mock(MetricService)
            service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
            service.reportService = Mock(ReportService)
            service.notificationService = Mock(NotificationService)
            service.workflowService = Mock(WorkflowService)
            def authorizingExec = Mock(AuthorizingExecution){
                1 * access(RundeckAccess.Execution.APP_KILL) >> new Execution()
                1 * authorize(RundeckAccess.Execution.APP_KILLAS) >> {
                    throw new UnauthorizedAccess('killAs', 'execution', 'blah')
                }
            }
        when:
            def result = service.abortExecution(authorizingExec, 'user1', false)
        then:
            UnauthorizedAccess t = thrown()
            t.action=='killAs'
    }

    def "get NodeService from Context"() {
        given:

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * getProjectGlobals(*_) >> [:]
            0 * _(*_)
        }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }
        service.rundeckNodeService = Mock(NodeService){}

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
        val.getNodeService() != null

    }

    def "loadSecureOptionStorageDefaultsWithOptionParam"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-env '+env,
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
                                name: 'env',
                                required: true
                        ),
                        new Option(
                                name: 'pass',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: 'keys/${option.env}/pass'
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        Map secureOpts = [:]
        def authContext = Mock(AuthContext)
        Map<String, String> args = FrameworkService.parseOptsFromString(job.argString)
        service.storageService = Mock(StorageService)


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext,false, args)

        then:
        service.storageService.storageTreeWithContext(authContext) >> Mock(KeyStorageTree) {
            hasPassword('keys/opta/pass')>>{
                return true
            }
            readPassword('keys/opta/pass') >> {
                    return 'pass1'.bytes
            }
            hasPassword('keys/optb/pass')>>{
                return true
            }
            readPassword('keys/optb/pass') >> {
                    return 'pass2'.bytes
            }
            hasPassword('keys/optc/pass')>>{
                return true
            }
            readPassword('keys/optc/pass') >> {
                    return 'pass3'.bytes
            }
            hasPassword(_) >> {
                return true
            }
            readPassword(_) >> {
                return ''.bytes
            }
        }
        expectedpass == secureOpts['pass']
        where:
        expectedpass    | env
        'pass1'         | 'opta'
        'pass2'         | 'optb'
        'pass3'         | 'optc'
        ''              | 'other'

    }

    def "cleanup execution should set custom state"() {
        given:
        Execution e = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false,
                dateStarted: new Date(),
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ).save(),
                ).save(flush: true)
        service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        service.reportService = Mock(ReportService) {
            1 * reportExecutionResult(_) >> [:]
        }
        service.notificationService = Mock(NotificationService) {
            1 * asyncTriggerJobNotification(_, _, _)
        }
        service.metricService = Mock(MetricService)
        service.workflowService = Mock(WorkflowService)
        when:
        service.cleanupExecution(e, status)
        then:
        e.refresh()
        e.id != null
        e.status == result
        e.cancelled == (status == null)

        where:
        status      | result
        'testvalue' | 'testvalue'
        null        | 'false'

    }

    def "get NodeService from origContext only if exists for referenced from another projects jobs"() {
        given:

        def orgProject = 'prgProj'
        def jobProj = 'testproj'

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, orgProject)
            0 * filterNodeSet(null, jobProj)
            1 * getProjectGlobals(*_) >> [:]
            0 * _(*_)
        }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }
        service.rundeckNodeService = Mock(NodeService){}

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: jobProj,
                loglevel: 'WARN',
                doNodedispatch: false
        )

        def origContext = Mock(StepExecutionContext){
            getFrameworkProject() >> orgProject
            getStepContext() >> []

        }

        when:
        def val = service.createContext(se, origContext, null, null, null, null, null,
        null, null, null, null, null, null, null, false)
        then:
        val != null
        val.getNodeService() != null

    }

    def "parent job fails if the job ref goes timeout"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
                jobName: jobname,
                project: project,
                groupPath: group,
                timeout: '3s',
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'sleep 10']
                        )]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
            _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                nodeSet
            }
            authorizeProjectJobAll(*_) >> true
        }
        service.frameworkService = Mock(FrameworkService) {

        }
        service.notificationService = Mock(NotificationService)
        def framework = Mock(Framework)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> framework

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )

        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>false
        }
        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult,interrupt:true]
            }
        }
        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def res = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        res instanceof StepExecutionResultImpl
        !res.success


    }

    def "parent job fails if the job ref with enforced allowed values has non allowed values"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        Option opt = new Option(name: 'test1', enforced: true, optionValues: null)
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-test1 b',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'echo A']
                        )]
                ),
                retry: '1'
        )
        job.save()
        job.addToOptions(opt)
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                1 * authorizeProjectJobAll(*_) >> true
            }
        service.frameworkService = Mock(FrameworkService)


        service.notificationService = Mock(NotificationService)
        def executionListener = Mock(ExecutionListener)
        def framework = Mock(Framework)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> framework
            getExecutionListener() >> executionListener

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['-test1', 'Asdd'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )

        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>false
        }
        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult,interrupt:true]
            }
        }
        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def res = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,_,_) >> {
            [
                    optionSelect : opt,
                    values       : ["A", "B", "C"],
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }

        1 * executionListener.log(_,_)
        res instanceof StepExecutionResultImpl
        !res.success
        res.failureMessage == "Invalid options: [test1]"


    }

    def "notification invocation on job ref"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                    nodeSet
                }
                authorizeProjectJobAll(*_) >> true
            }
        service.frameworkService = Mock(FrameworkService) {

        }

            service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)



        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>success
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        1 * service.notificationService.asyncTriggerJobNotification('start', _, _)
        1 * service.notificationService.asyncTriggerJobNotification(trigger, _, _)
        where:
        success      | trigger
        true         | 'success'
        false        | 'failure'
    }

    def "export variables on job ref are added to output context"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        def sharedContext = SharedDataContextUtils.sharedContext()
        sharedContext.merge(ContextView.global(), DataContextUtils.context("export", [globular: "globalvalue"]))

        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test', argString: '-delay 12'])]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id], data:[testVar: "test"]]
        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
            _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                nodeSet
            }
            authorizeProjectJobAll(*_) >> true
        }
        service.frameworkService = Mock(FrameworkService) {

        }

        service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)
            getOutputContext()>>new DataOutput(ContextView.global())

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                ignoreNotifications,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>success
            getSharedContext() >> Mock(WFSharedContext) {
                consolidate()>>sharedContext
            }
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        origContext.getOutputContext().getSharedContext().getData(ContextView.global()) == ["export":[globular: "globalvalue"]]

        where:
        success      | ignoreNotifications
        true         | true
        false        | true
        true         | false
        false        | false
    }

    def "respect disabled execution on job ref"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                executionEnabled: false
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                    nodeSet
                }
                authorizeProjectJobAll(*_) >> true
            }
        service.frameworkService = Mock(FrameworkService) {

        }



        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                failOnDisable,
                false,
                null,
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)

        service.metricService = Mock(MetricService)

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def result = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * service.metricService.withTimer(_,_,_)

        result.success == expectedSucces
        where:
        failOnDisable   | expectedSucces
        true            | false
        false           | true
    }

    void "create execution dynamic threadcount from option"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                nodeThreadcountDynamic: "\${option.threadCount}",
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()

        def opt1 = new Option(name: 'threadCount', enforced: false, required: false, defaultValue: "10")

        assert job.validate()

        job.addToOptions(opt1)
        null != job.save()


        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                [executionType: 'user']
        )

        then:
        e2 != null
        e2.nodeThreadcount == 10

    }

    void "create execution dynamic threadcount from value"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                nodeThreadcountDynamic: "15",
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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                [ executionType: 'user']
        )

        then:
        e2 != null
        e2.nodeThreadcount == 15

    }

    void "wrong execution dynamic threadcount from option"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                nodeThreadcountDynamic: "\${option.threadCount}",
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()

        def opt1 = new Option(name: 'threadCount', enforced: false, required: false, defaultValue: "wrongthreadcountvalue")

        assert job.validate()

        job.addToOptions(opt1)
        null != job.save()


        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                [executionType: 'user']
        )

        then:
        e2 != null
        e2.nodeThreadcount == 1

    }

    def "execut job ref from uuid"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                    nodeSet
                }
                authorizeProjectJobAll(*_) >> true
            }
        service.frameworkService = Mock(FrameworkService) {
        }
            def executionListener = Mock(ExecutionListener)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)
            getExecutionListener() >> executionListener

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                null,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                null,
            null,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
                )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>true
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def ret = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * executionListener.log(_)
        ret.success
    }


    def "disable stats for job ref execution by framework property"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (['rundeck.disable.ref.stats': propValue] as Properties)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor) {
                _ * filterAuthorizedNodes(_, _, _, _) >> { args ->
                    nodeSet
                }
                authorizeProjectJobAll(*_) >> true
            }

            def executionListener = Mock(ExecutionListener)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)
            getExecutionListener() >> executionListener

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                null,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>true
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def ret = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        def refexec = ReferencedExecution.findByJobUuid(job.uuid)
        def seStats = ScheduledExecutionStats.findByJobUuid(job.uuid)
        if(expectedRef){
            seStats.getContentMap().refExecCount==0
        }else{
            seStats.getContentMap().refExecCount==1
        }
        0 * executionListener.log(_)
        ret.success
        where:
        propValue | expectedRef
        'true'    | false
        'false'   | true
        ''        | true
    }
    def "createJobReferenceContext import options"() {
        given:
        def context = ExecutionContextImpl.builder().
            threadCount(1).
            keepgoing(false).
            dataContext(['option': ['monkey': 'wakeful'], 'secureOption': [:], 'job': ['execid': '123']]).
            privateDataContext(['option': [:],]).
            user('aUser').
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
        def opt1 = new Option(name: 'monkey', enforced: false, required: false)
        def opt2 = new Option(name: 'delay', enforced: false, required: false)
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
            se,
            null,
            context,
            ['-test3', 'fix'] as String[],
            null, null, null, null, null, null, false,
            importOptions,
            true, false
        )

        then:
        newCtxt.dataContext.option
        expectedSize == newCtxt.dataContext.option.size()

        where:
        importOptions | expectedSize
        true          | 2
        false         | 1
    }


    def "loadSecureOptionStorageDefaults replace job vars"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
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
                                name: 'pass',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: 'keys/${job.username}/pass'
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        Map secureOpts = [:]
        def authContext = Mock(AuthContext)
        Map<String, String> args = FrameworkService.parseOptsFromString(job.argString)
        service.storageService = Mock(StorageService)

        def jobcontext = [:]
        jobcontext.id = job.extid
        jobcontext.name = job.jobName
        jobcontext.group = job.groupPath
        jobcontext.project = job.project
        jobcontext.username = username
        jobcontext['user.name'] = jobcontext.username


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext,false,
                args,jobcontext)

        then:
        service.storageService.storageTreeWithContext(authContext) >> Mock(KeyStorageTree) {
            hasPassword('keys/admin/pass') >> true
            readPassword('keys/admin/pass') >> {
                return 'pass1'.bytes
            }
            hasPassword('keys/dev/pass') >> true
            readPassword('keys/dev/pass') >> {
                return 'pass2'.bytes
            }
            hasPassword('keys/op/pass') >> true
            readPassword('keys/op/pass') >> {
                return 'pass3'.bytes
            }
            hasPassword(_) >> {
                return true
            }
            readPassword(_) >> {
                return ''.bytes
            }
        }
        expectedpass == secureOpts['pass']
        where:
        expectedpass    | username
        'pass1'         | 'admin'
        'pass2'         | 'dev'
        'pass3'         | 'op'
        ''              | 'user'

    }


    def "loadSecureOptionStorageDefaults node deferred variables"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
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
                                name: 'pass',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: securepath
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        Map secureOpts = [:]
        def authContext = Mock(AuthContext)
        Map<String, String> args = FrameworkService.parseOptsFromString(job.argString)
        service.storageService = Mock(StorageService)

        def jobcontext = [:]
        jobcontext.id = job.extid
        jobcontext.name = job.jobName
        jobcontext.group = job.groupPath
        jobcontext.project = job.project
        jobcontext.username = 'admin'
        jobcontext['user.name'] = jobcontext.username

        def secureOptionNodeDeferred = [:]


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext,false,
                args,jobcontext,secureOptionNodeDeferred)

        then:
        secureOptionNodeDeferred
        secureOptionNodeDeferred.size()==1
        secureOptionNodeDeferred['pass']
        secureOptionNodeDeferred['pass']==securepath

        where:
        securepath                         | _
        'keys/${node.hostname}/pass'       | _
        'keys/${node.username}/pass'       | _

    }

    def "createJobReferenceContext file type option"() {
        given:
        def context = ExecutionContextImpl.builder().
                threadCount(1).
                keepgoing(false).
                dataContext(['option': ['monkey': 'wakeful', 'file':'0000000'], 'secureOption': [:], 'job': ['execid': '123']]).
                privateDataContext(['option': [:],]).
                user('aUser').
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
        def opt1 = new Option(name: 'monkey', enforced: false, required: false)
        def opt2 = new Option(name: 'delay', enforced: false, required: false)
        def opt3 = new Option(name: 'file', required: true, enforced: false, optionType: 'file')
        assert opt3.validate()
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        se.addToOptions(opt3)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }

        service.fileUploadService = Mock(FileUploadService) {
            1 * validateFileRefForJobOption(_, _, _, _) >> [
                    valid: true
            ]
            1 * executionBeforeStart(_,true)
        }
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-test3', 'fix'] as String[],
                null, null, null, null, null, null, false,
                importOptions,
                true, false
        )

        then:
        newCtxt.dataContext.option
        expectedSize == newCtxt.dataContext.option.size()

        where:
        importOptions | expectedSize
        true          | 3
    }

    def "createJobReferenceContext default values"() {
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
        def opt1 = new Option(name: 'test1', enforced: false, required: false, defaultValue: "test123.")
        assert opt1.validate()
        se.addToOptions(opt1)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],
                null, null, null, null, null, null, false, false, true, false
        )

        then:
        newCtxt.dataContext['option'] == ['test1': 'test123.']
    }

    def "createJobReferenceContext default multi-values"() {
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
        def opt1 = new Option(name: 'test1', multivalued: true, delimiter: "," , enforced: false, required: false, defaultValue: "A,B")
        assert opt1.validate()
        se.addToOptions(opt1)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],
                null, null, null, null, null, null, false, false, true,false
        )

        then:
        newCtxt.dataContext['option'] == ['test1.delimiter':',', 'test1':'A,B']

    }


    def "execut job ref empty node filter successOnEmptyNodeFilter false"(){
        given:
        def jobname = 'refjobx'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'x job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                successOnEmptyNodeFilter: false

        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    new NodeSetImpl()
                }
            }

            def executionListener = Mock(ExecutionListener)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> new NodeSetImpl()
            getFramework() >> Mock(Framework)
            getExecutionListener() >> executionListener

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                null,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>true
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def ret = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        thrown StepException
    }

    def "execut job ref empty node filter successOnEmptyNodeFilter true"(){
        given:
        def jobname = 'refjobx'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'x job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                successOnEmptyNodeFilter: true

        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    new NodeSetImpl()
                }
            }

            def executionListener = Mock(ExecutionListener)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> new NodeSetImpl()
            getFramework() >> Mock(Framework)
            getExecutionListener() >> executionListener

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                null,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                true,
                null,
                null,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>true
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def ret = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * executionListener.log(_)
        ret.success
    }

    def "createJobReferenceContext default values secure option"() {
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
                options: [
                        new Option(
                                name: 'pass',
                                secureInput: true,
                                secureExposed: true,
                                defaultStoragePath: "keys/admin/pass"
                        )
                ]
                )
        null != se
        null != se.save()
        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)>>{
                    new NodeSetImpl()
                }
            }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)
        def authContext = Mock(AuthContext)
        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                [] as String[],
                null, null, null, null, null, null, false, false, false, false
        )

        then:

        2*service.storageService.storageTreeWithContext(_) >> Mock(KeyStorageTree) {
            hasPassword('keys/admin/pass')>>true
            readPassword('keys/admin/pass') >> {
                return 'pass1'.bytes
            }
        }

        newCtxt.dataContext['secureOption'] == ['pass': 'pass1']
    }

    def "createJobReferenceContext secure opts default storage path resolve reference"() {
        given:
        def context = ExecutionContextImpl.builder()
                .
                threadCount(1)
                .
                keepgoing(false)
                .
                dataContext(['option': ['optionparent': 'pass'], 'secureOption': [:], 'job': ['execid': '123']])
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
                name: 'optionchild',
                enforced: false,
                required: true,
                secureInput: false,
                default: 'passchild'
        )
        def opt2 = new Option(
                name: 'password',
                enforced: false,
                required: false,
                secureInput: true,
                secureExposed: true,
                defaultStoragePath: 'keys/${option.optionchild}/myPassword'
        )
        assert opt1.validate()
        assert opt2.validate()
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'AProject')
            1 * getProjectGlobals(*_)
            0 * _(*_)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                1 * filterAuthorizedNodes(*_)>>{
                    new NodeSetImpl()
                }
            }

        service.fileUploadService = Mock(FileUploadService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.storageService = Mock(StorageService)

        when:

        def newCtxt = service.createJobReferenceContext(
                se,
                null,
                context,
                ['-optionchild', 'pass'] as String[],//null values for the input options
                null, null, null, null, null, null, false, false, true, false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['password': 'newtest1']
        newCtxt.dataContext['option'] == ['optionchild': 'pass', 'password':'newtest1']

        service.storageService.storageTreeWithContext(_) >> Mock(KeyStorageTree) {
            1 * hasPassword('keys/pass/myPassword') >> true
            1 * readPassword('keys/pass/myPassword') >> {
                return 'newtest1'.bytes
            }
        }
    }

    def "notification invocation on job ref pass ScheduledExecution directly"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }
        service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)



        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>success
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        1 * service.notificationService.asyncTriggerJobNotification('start', job.uuid, _)
        1 * service.notificationService.asyncTriggerJobNotification(trigger, job.uuid, _)
        where:
        success      | trigger
        true         | 'success'
        false        | 'failure'
    }

    void "create execution exclude filter"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                doNodedispatch: true,
                filter:'tags: running',
                filterExclude:'name: nodea',
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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['extra.option.test': '12', executionType: 'user']
        )

        then:
        e2 != null
        e2.filterExclude == 'name: nodea'
    }


    def "Create execution context with exclude filter"() {
        given:

        service.frameworkService = Mock(FrameworkService)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: true,
                filter:'tags: running',
                filterExclude:'name: nodea'
        )

        when:
        def val = service.createContext(se, null, null, null, null, null, null)
        then:
        val != null
        val.nodeSelector != null
        val.nodeSelector.excludes.name == "nodea"
        val.frameworkProject == "testproj"
    }

    def "get Global Plugin Configurations"() {

        given:
        def project = "asdf"
        service.frameworkService = Stub(FrameworkService) {
            getProjectProperties(project) >> props
        }
        when:

        //def result = ProjectNodeSupport.listPluginConfigurations(props, prefix, svc, true)
        def result = service.getGlobalPluginConfigurations(project)
        then:

        result.size() == expectedSize

        where:
        expectedSize    | props
        1               | ['framework.globalfilter.1.type':'mask-passwords',
                           'framework.globalfilter.1.config.replacement':'[SECURE]',
                           'framework.globalfilter.1.config.color': 'blue']
        1               | ['project.globalfilter.1.type':'highlight-output',
                           'project.globalfilter.1.config.regex':'test',
                           'project.globalfilter.1.config.bgcolor': 'yellow']
        0               | [:]
        2               | ['framework.globalfilter.1.type':'mask-passwords',
                           'framework.globalfilter.1.config.replacement':'[SECURE]',
                           'framework.globalfilter.1.config.color': 'blue',
                           'project.globalfilter.1.type':'highlight-output',
                           'project.globalfilter.1.config.regex':'test',
                           'project.globalfilter.1.config.bgcolor': 'yellow']
        2               | ['framework.globalfilter.1.type':'mask-passwords',
                           'framework.globalfilter.1.config.replacement':'[SECURE]',
                           'framework.globalfilter.1.config.color': 'blue',
                           'framework.globalfilter.2.type':'highlight-output',
                           'framework.globalfilter.2.config.regex':'test',
                           'framework.globalfilter.2.config.bgcolor': 'yellow']

    }

    void "runnow execution with exclude filter"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                description: 'a job',
                doNodedispatch: true,
                filter:'tags: running',
                filterExclude:'name: nodea',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                )
        )
        job.save()

        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['_replaceNodeFilters': 'true', 'nodeIncludeName':'SelectedNode', executionType: 'user','nodeoverride':'cherrypick']
        )

        then:
        e2 != null
        e2.filter == "name: SelectedNode"
        e2.filterExclude == null
    }

    def "skip notification property on job ref"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }
            service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)


        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                true,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>success
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * service.notificationService.asyncTriggerJobNotification('start', _, _)
        0 * service.notificationService.asyncTriggerJobNotification(trigger, _, _)
        where:
        success      | trigger
        true         | 'success'
        false        | 'failure'
    }

    def "parent job fails if the job ref goes timeout using option variable"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                timeout: '${option.timeout}',
                description: 'a job',
                argString: '-args b -timeout 3s',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'sleep 10']
                        )]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }
            service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)

        service.notificationService = Mock(NotificationService)
        def framework = Mock(Framework)

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> framework

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'timeout'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )

        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>false
        }
        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult,interrupt:true]
            }
        }
        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def res = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        1 * service.executionUtilService.runRefJobWithTimer(_, _, _, 3000)
        res instanceof StepExecutionResultImpl
        !res.success
    }

    def "Create execution context with global vars and option value replacement"() {
        given:

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * getProjectGlobals(*_) >> [a: 'b', c: 'd']
            0 * _(*_)
        }
            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }

        Execution se = new Execution(
                argString: "-test \${globals.a}",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false
        )

        when:
        def val = service.createContext(se, null, null, null, null, null, null)
        then:
        val != null
        val.dataContext.option.test == 'b'
        val.nodeSelector == null
        val.frameworkProject == "testproj"
        "testuser" == val.user
        1 == val.loglevel
        !val.executionListener
        val.dataContext.globals == [a: 'b', c: 'd']
    }

    def "Create execution context with global vars and option value replacement not found"() {
        given:

        service.frameworkService = Mock(FrameworkService) {
            1 * filterNodeSet(null, 'testproj')
            1 * getProjectGlobals(*_) >> [a: 'b', c: 'd']
            0 * _(*_)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }

        Execution se = new Execution(
                argString: "-test \${globals.test}",
                user: "testuser",
                project: "testproj",
                loglevel: 'WARN',
                doNodedispatch: false
        )

        when:
        def val = service.createContext(se, null, null, null, null, null, null)
        then:
        val != null
        val.dataContext.option.test == '\${globals.test}'
        val.nodeSelector == null
        val.frameworkProject == "testproj"
        "testuser" == val.user
        1 == val.loglevel
        !val.executionListener
        val.dataContext.globals == [a: 'b', c: 'd']
    }

    def "execute job with secure remote option changed by job life cycle" () {
        given:
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService){
            beforeJobExecution(_,_) >> Mock(JobLifecycleStatus){
                1 * isUseNewValues() >> true
                2 * getOptionsValues() >> ["securedOption1" : "secured option changed value"]
            }
        }

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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }

        def securedOpts = ["securedOption1" : "secured option original value"]
        def securedExposedOpts = ["securedExposedOption1" : "secured exposed option original value"]
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['extra.option.test': '12', executionType: 'user'],
                false,
                -1,
                securedOpts,
                securedExposedOpts
        )

        then:
        e2 != null
        securedOpts.get("securedOption1") == "secured option changed value"
        securedOpts.size() == 1
        securedExposedOpts.get("securedExposedOption1") == "secured exposed option original value"
        securedExposedOpts.size() == 1
        e2.user == 'testuser'
    }

    def "execute job with secure exposed option changed by job life cycle" () {
        given:
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService){
            beforeJobExecution(_,_) >> Mock(JobLifecycleStatus){
                1 * isUseNewValues() >> true
                2 * getOptionsValues() >> ["securedExposedOption1" : "secured exposed option changed value"]
            }
        }

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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }

        def securedOpts = ["securedOption1" : "secured option original value"]
        def securedExposedOpts = ["securedExposedOption1" : "secured exposed option original value"]
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['extra.option.test': '12', executionType: 'user'],
                false,
                -1,
                securedOpts,
                securedExposedOpts
        )

        then:
        e2 != null
        securedOpts.get("securedOption1") == "secured option original value"
        securedOpts.size() == 1
        securedExposedOpts.get("securedExposedOption1") == "secured exposed option changed value"
        securedExposedOpts.size() == 1
        e2.user == 'testuser'
    }


    def "execute job with secure remote and exposed options changed by job life cycle" () {
        given:
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService){
            beforeJobExecution(_,_) >> Mock(JobLifecycleStatus){
                1 * isUseNewValues() >> true
                2 * getOptionsValues() >> ["securedExposedOption1" : "secured exposed option changed value",
                                           "securedOption1" : "secured option changed value"]
            }
        }

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
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            getNodes(_,_) >> null
        }

        def securedOpts = ["securedOption1" : "secured option original value"]
        def securedExposedOpts = ["securedExposedOption1" : "secured exposed option original value"]
        when:
        Execution e2 = service.createExecution(
                job,
                authContext,
                'testuser',
                ['extra.option.test': '12', executionType: 'user'],
                false,
                -1,
                securedOpts,
                securedExposedOpts
        )

        then:
        e2 != null
        securedOpts.get("securedOption1") == "secured option changed value"
        securedOpts.size() == 1
        securedExposedOpts.get("securedExposedOption1") == "secured exposed option changed value"
        securedExposedOpts.size() == 1
        e2.user == 'testuser'
    }

    def "can read storage password using variables"() {
        given:
        AuthContext context = Mock(AuthContext)
        service.storageService = Mock(StorageService)

        when:
        def result = service.canReadStoragePassword(context, path, false)

        then:
        service.storageService.storageTreeWithContext(context) >> Mock(KeyStorageTree) {
            0 * hasPassword(path)
        }
        result == canread

        where:
        path                            | canread
        'keys/${job.username}/password' | true

    }



    def "abort run on aborted execution"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                abortedby: 'admin'
        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }
            service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)


        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                group+'/'+jobname,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                project,
                false,
                false,
                null,
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>success
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                closure()
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * service.notificationService.asyncTriggerJobNotification('start', _, _)
        1 * service.notificationService.asyncTriggerJobNotification(trigger, _, _)
        where:
        success      | trigger
        true         | 'success'
        false        | 'failure'
    }

    def "use option vars on remoteurl validation"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: 'test1', enforced: true, optionValues: null)
        se.addToOptions(opt)
        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        when:

        service.validateOptionValues(se, opts)

        then:
        1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,
                ['option':'test1', 'extra':['option':['test1':'Foo']]],_,_) >> {
            [
                    optionSelect : opt,
                    values       : remoteValues,
                    srcUrl       : "cleanUrl",
                    err          : null
            ]
        }

        noExceptionThrown()


        where:
        opts                                           | remoteValues
        ['test1': 'Foo']                               | ["Foo", "Bar"]
    }

    def "get effective node succedeed list"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                abortedby: 'admin',
                workflow: job.workflow
        )
        e1.scheduledExecution = job
        e1.save() != null

        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        def node2 = new NodeEntryImpl('node2')
        def node3 = new NodeEntryImpl('node3')

        nodeSet.putNode(node1)
        nodeSet.putNode(node2)
        nodeSet.putNode(node3)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {

        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                authorizeProjectJobAll(*_) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }

        service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)
        service.workflowService = Mock(WorkflowService)
        service.notificationService = Mock(NotificationService)
        service.reportService = Mock(ReportService){
            reportExecutionResult(_) >> [:]
        }

        HashSet<String> nodeNames = new HashSet<>(nodeSet.getNodeNames());
        HashMap<String, NodeStepResult> failures = new HashMap<>();
        NodeRecorder recorder = new NodeRecorder()
        recorder.matchedNodes(nodeNames)

        NodeStepResult result = Mock(NodeStepResult)

        failureList.split(",").each {failedNode->
            failures.put(failedNode, result)
        }

        recorder.nodesFailed(failures)

        ExecutionService.AsyncStarted execmap = new ExecutionService.AsyncStarted(
                noderecorder      : recorder,
                execution         : e1,
                scheduledExecution: job
        )

        Map<String, Object> failedNodes = recorder.getFailedNodes()
        Set<String> succeededNodes = recorder.getSuccessfulNodes()

        Map resultMap = [
                status        : success? ExecutionState.succeeded.toString():ExecutionState.failed.toString(),
                dateCompleted : new Date(),
                cancelled     : false,
                timedOut      : false,
                failedNodes   : failedNodes?.keySet(),
                failedNodesMap: failedNodes,
                succeededNodes: succeededNodes,
        ]


        when:
        service.saveExecutionState(job.uuid, e1.id, resultMap, execmap, [:])
        then:

        calls * service.workflowService.requestStateSummary(_,succeededNodes.toList()) >> new WorkflowStateFileLoader(workflowState: [nodeSummaries: nodeSummaries])

        e1.succeededNodeList == effectiveSuccessNodeList?.join(",")

        where:
        failureList             | calls | nodeSummaries                                                                         |  effectiveSuccessNodeList      | success
        "node2"                 | 1     | [node1: [summaryState:'SUCCEEDED'], node3: [summaryState:'PENDING']]                  |  ["node1"]                     | false
        "node2,node3"           | 1     | [node1: [summaryState:'SUCCEEDED']]                                                   |  ["node1"]                     | false
        "node1,node2,node3"     | 0     | []                                                                                    |  null                          | false
        ""                      | 1     | [node1: [summaryState:'SUCCEEDED'], node2: [summaryState:'SUCCEEDED'], node3: [summaryState:'SUCCEEDED']]   | ["node2","node3","node1"] | true
    }

    def "opt enforced allowed values from Remote Url with sending the username"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        Option opt = new Option(name: null, enforced: true, defaultValue: null, optionValues: null)
        se.addToOptions(opt)

        UserAndRolesAuthContext admin = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'Admin'
            getRoles() >> new HashSet<String>(['Admin'])
        }

        service.scheduledExecutionService = Mock(ScheduledExecutionService)

        when:
        HashMap optparams = service.parseJobOptionInput([:], se, admin)

        then:
            1 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_,'Admin',_) >> [:]
            0 * service.scheduledExecutionService.loadOptionsRemoteValues(_,_, null,_) >> [:]
    }


    def "validate permission on job from another project"(){
        given:
        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a job',
                argString: '-args b -args2 d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d431-b70a-42ad-8ea8-37ad4885ea0d'
        )
        job.save()
        Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful'

        )
        e1.save() != null

        def datacontext = [job:[execid:e1.id]]


        def nodeSet = new NodeSetImpl()
        def node1 = new NodeEntryImpl('node1')
        nodeSet.putNode(node1)

        def refAuth = Mock(SubjectAuthContext)

        service.fileUploadService = Mock(FileUploadService)
        service.executionUtilService = Mock(ExecutionUtilService)
        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        service.frameworkService = Mock(FrameworkService) {
            }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * getAuthContextForUserAndRolesAndProject(_,_,project)>>refAuth
                authorizeProjectJobAll(refAuth, _, [AuthConstants.ACTION_RUN], project) >> true
                _ * filterAuthorizedNodes(*_)>>{
                    nodeSet
                }
            }


        def executionListener = Mock(ExecutionListener)
        def userAndRolesAuthContext = Mock(SubjectAuthContext){
            getUsername()>> 'username'
            getRoles()>>['rol']
        }

        def origContext = Mock(StepExecutionContext){
            getDataContext()>>datacontext
            getStepNumber()>>1
            getStepContext()>>[]
            getNodes()>> nodeSet
            getFramework() >> Mock(Framework)
            getExecutionListener() >> executionListener
            getUserAndRolesAuthContext()> userAndRolesAuthContext

        }
        JobRefCommand item = ExecutionItemFactory.createJobRef(
                null,
                ['args', 'args2'] as String[],
                false,
                null,
                true,
                '.*',
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                'bd80d431-b70a-42ad-8ea8-37ad4885ea0d',
                false,
                false,
                false
        )


        service.notificationService = Mock(NotificationService)
        def dispatcherResult = Mock(DispatcherResult)
        def wresult = Mock(WorkflowExecutionResult){
            isSuccess()>>true
        }


        service.metricService = Mock(MetricService){
            withTimer(_,_,_)>>{classname, name,  closure ->
                [result:wresult]
            }
        }

        def createFailure = { FailureReason reason, String msg ->
            return new StepExecutionResultImpl(null, reason, msg)
        }
        def createSuccess = {
            return new StepExecutionResultImpl()
        }
        when:
        def ret = service.runJobRefExecutionItem(origContext,item,createFailure,createSuccess)
        then:
        0 * executionListener.log(_)
        ret.success
    }

    def "create missed execution"() {
        given:
        Date scheduledTime = new Date(Instant.now().minus(Duration.of(10, ChronoUnit.SECONDS)).toEpochMilli());
        def jobname = 'scheduled job'
        def group = 'scheduled'
        def project = 'proj'
        ScheduledExecution job = new ScheduledExecution(
                jobName: jobname,
                project: project,
                groupPath: group,
                description: 'a scheduled job',
                user: 'user',
                userRoleList: 'role1,role2',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'echo hello']
                        )]
                ),
                retry: '1',
                uuid: 'bd80d444-0700-42ad-8ea8-37ad4885ea0d'
        )
        if(notification) {
            job.addToNotifications(notification)
            def authContext = Mock(UserAndRolesAuthContext)
            service.frameworkService = Mock(FrameworkService)
            service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                getAuthContextForUserAndRolesAndProject(_,_,_)>>authContext
            }
            service.storageService = Mock(StorageService) {
                storageTreeWithContext(_)>>Mock(KeyStorageTree)
            }
        }
        job.save()
        service.notificationService = Mock(NotificationService)
        when:
        service.createMissedExecution(job,"201365e7-2222-433d-a4d9-0d7712df0f84",scheduledTime)
        Execution execution = Execution.findByScheduledExecution(job)
        ExecReport execReport = ExecReport.findByJobId(job.id.toString())

        then:
        triggerNotificationCalled * service.notificationService.triggerJobNotification(_,_,_)
        execution.project == project
        execution.user == job.user
        execution.userRoleList == job.userRoleList
        execution.dateStarted == scheduledTime
        execution.dateCompleted > execution.dateStarted
        execution.executionState == "missed"
        execReport.status == "missed"

        where:
        triggerNotificationCalled | notification
        0 | null
        1 | new Notification(eventTrigger: "onfailure",type: "url",format:"json",content: "http://mywebhook.com")
    }

      def "use referenced job project if enabled"() {
        given:

        def orgProject = 'prgProj'
        def jobProj = 'testproj'
        service.frameworkService = Mock(FrameworkService) {
            0 * filterNodeSet(null, orgProject)
            1 * filterNodeSet(null, jobProj)
            1 * getProjectGlobals(*_) >> [:]
            0 * _(*_)
        }

            service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
                1 * filterAuthorizedNodes(*_)
            }
        service.storageService = Mock(StorageService) {
            1 * storageTreeWithContext(_)
        }
        service.jobStateService = Mock(JobStateService) {
            1 * jobServiceWithAuthContext(_)
        }
        service.rundeckNodeService = Mock(NodeService){}

        Execution se = new Execution(
                argString: "-test args",
                user: "testuser",
                project: jobProj,
                loglevel: 'WARN',
                doNodedispatch: false
        )

        def origContext = Mock(StepExecutionContext){
            getFrameworkProject() >> orgProject
            getStepContext() >> []

        }

        when:
        def val = service.createContext(se, origContext, null, null, null, null, null,
                null, null, null, null, null, null, null, true)
        then:
        val != null
        val.getNodeService() != null
      }

    def "metrics data from criteria result"(){
        given:
            def critresult=[
                count:3,
                durationMax: new Long(1L),
                durationMin: new Long(2L),
                durationSum: new Long(3L),
            ]
        when:
            def result = service.metricsDataFromCriteriaResult(critresult)
        then:
        result.total == 3
        result.duration.average == 1000L
        result.duration.min == 75602000L
        result.duration.max == 75601000L
    }

    def "metrics data from projection result"(){
        given:
            Date now = new Date()
            Date dateStarted1 = now
            Date dateStarted2 = now
            Date dateStarted3 = now
            Date dateCompleted = now
            use (TimeCategory) {
                dateStarted1 = now - 9.minute
                dateStarted2 = now - 4.minute
                dateStarted3 = now - 2.minute
            }
            def projresult = [
                [dateStarted: new Timestamp(dateStarted1.time), dateCompleted: new Timestamp(dateCompleted.time)],
                [dateStarted: new Timestamp(dateStarted2.time), dateCompleted: new Timestamp(dateCompleted.time)],
                [dateStarted: new Timestamp(dateStarted3.time), dateCompleted: new Timestamp(dateCompleted.time)]
            ]
        when:
            def result = service.metricsDataFromProjectionResult(projresult)
        then:
            result.total == 3
            result.duration.average == 5L * 60L * 1000L
            result.duration.min == 2L * 60L * 1000L
            result.duration.max == 9L * 60L * 1000L
    }

    def "load additional listener"(){
        when:
            def result = service.loadAdditionalListeners([])
        then:
            result.size() == 1
            result[0].class.name == WorkflowExecutionListenerTest.class.name
    }

    def "should add additional listener to original listeners"() {
        given:
            def initialListeners = [
                    new LogFlusher(),
                    new LogFlusher()
            ]
        when:
            def result = service.loadAdditionalListeners(initialListeners)
        then:
            result.size() == 3
            result.containsAll(initialListeners)
    }

    def "should not fail if there is no additional listeners"() {
        given:
            ServiceLoader.metaClass.static.load = { Class clazz ->
                return null
            }
            def initialListeners = [
                    new LogFlusher(),
                    new LogFlusher()
            ]
        when:
            def result = service.loadAdditionalListeners(initialListeners)
        then:
            result.size() == 2
            result.containsAll(initialListeners)
    }
}

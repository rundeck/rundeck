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

package rundeck.controllers

import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest
import org.grails.plugins.metricsweb.MetricService
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.data.model.v1.report.RdExecReport
import org.rundeck.app.data.providers.GormReferencedExecutionDataProvider
import rundeck.*
import rundeck.services.FrameworkService
import rundeck.services.ReportService
import rundeck.services.UserService
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

/**
 * Created by greg on 9/22/16.
 */
class ReportsControllerSpec extends RundeckHibernateSpec implements ControllerUnitTest<ReportsController> {

    List<Class> getDomainClasses() { [Execution, ScheduledExecution, ReferencedExecution, CommandExec] }

    Closure doWithConfig() {{ config ->
        config.grails.databinding.dateFormats = [
                //default grails patterns
                //default grails patterns
                "yyyy-MM-dd HH:mm:ss.S",
                "yyyy-MM-dd'T'hh:mm:ss'Z'",

                // ISO8601 patterns
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        ]
    }}

    @Unroll
    def "events query date binding format #dateFilter"() {
        given:

        controller.frameworkService = Mock(FrameworkService)

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                _ * getAuthContextForSubjectAndProject(*_) >> null
                _ * authorizeProjectResource(*_) >> true
            }
        controller.userService = Mock(UserService) {
            findOrCreateUser(*_) >> new User()
        }

        Map<String, List> authorizations = [:]
        authorizations.put(ReportService.DENIED_VIEW_HISTORY_JOBS,[])
        controller.reportService = Mock(ReportService){
            jobHistoryAuthorizations(_,_) >> authorizations
        }


        when:
        params.doendafterFilter = 'true'
        params.endafterFilter = dateFilter
        def result = controller.eventsAjax()


        then:
        response.status == 200
        response.json.reports == []
        _ * controller.reportService.getExecutionReports({ ExecQuery query ->
            query.doendafterFilter && query.endafterFilter
                                                         }, true
        ) >> [reports: []]
        _ * controller.reportService.finishquery(_, _, _) >> { args -> args[2] }

        where:
        dateFilter                      | _
        '1999-01-01T01:23:45Z'          | _
        '1999-01-01 01:23:45.123'       | _
        '1999-01-01T13:23:45Z'          | _
        '1999-01-01T13:23:45-08'        | _
        '1999-01-01T13:23:45-0800'      | _
        '1999-01-01T13:23:45-08:00'     | _
        '1999-01-01T13:23:45.123Z'      | _
        '1999-01-01T13:23:45.123-08'    | _
        '1999-01-01T13:23:45.123-0800'  | _
        '1999-01-01T13:23:45.123-08:00' | _
    }

    interface ExtRdExecReport extends RdExecReport {
        Map toMap()
    }

    @Unroll
    def "events query should look up by id if report is missing execution uuid"() {
        given:

            controller.frameworkService = Mock(FrameworkService)

            controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
                _ * getAuthContextForSubjectAndProject(*_) >> null
                _ * authorizeProjectResource(*_) >> true
            }
            controller.userService = Mock(UserService) {
                findOrCreateUser(*_) >> new User()
            }

            Map<String, List> authorizations = [:]
            authorizations.put(ReportService.DENIED_VIEW_HISTORY_JOBS, [])
            controller.reportService = Mock(ReportService) {
                jobHistoryAuthorizations(_, _) >> authorizations
            }
            def project = 'test'
            def wf = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])

            Execution e1 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful',
                uuid:null,
                workflow: wf
            )
            e1.save(flush:true)
            def wf2 = new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])


            def uuid1 = UUID.randomUUID().toString()
            Execution e2 = new Execution(
                project: project,
                user: 'bob',
                dateStarted: new Date(),
                dateEnded: new Date(),
                status: 'successful',
                uuid: uuid1,
                workflow:wf2
            )
            e2.save(flush:true)
            def reports = [
                Mock(ExtRdExecReport) {
                    _*getDateStarted()>>e1.dateStarted
                    _*getDateCompleted()>>e1.dateCompleted
                    _*getExecutionUuid() >> null
                    _*getExecutionId() >> e1.id
                    _*toMap()>>[executionId:e1.id,executionUuid:e1.uuid, project:project]
                },
                Mock(ExtRdExecReport) {
                    _*getDateStarted()>>e2.dateStarted
                    _*getDateCompleted()>>e2.dateCompleted
                    _*getExecutionUuid() >> e2.uuid
                    _*getExecutionId() >> e2.uuid
                    _*toMap()>>[executionId:e2.id,executionUuid:e2.uuid, project:project]
                }
            ]


        when:
            params.projFilter = project
            def result = controller.eventsAjax()


        then:
            response.status == 200
            response.json.reports.size() == 2
            response.json.reports[0].executionId=='1'
            response.json.reports[0].executionUuid==null
            response.json.reports[1].executionId=='2'
            response.json.reports[1].executionUuid==uuid1

            _ * controller.reportService.getExecutionReports(_, _) >> [reports: reports]
            _ * controller.reportService.finishquery(_, _, _) >> { args -> args[2] }

    }
    @Unroll
    def "add job ref on index"() {
        given:

        controller.frameworkService = Mock(FrameworkService)
        controller.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            1 * getAuthContextForSubjectAndProject(_, _)
            _ * authorizeProjectResource(*_) >> true
        }
        controller.userService = Mock(UserService) {
            findOrCreateUser(*_) >> new User()
        }

        Map<String, List> authorizations = [:]
        authorizations.put(ReportService.DENIED_VIEW_HISTORY_JOBS,[])
        controller.reportService = Mock(ReportService){
            jobHistoryAuthorizations(_,_) >> authorizations
        }
        controller.metricService = Mock(MetricService)
        controller.referencedExecutionDataProvider = new GormReferencedExecutionDataProvider()

        def jobname = 'abc'
        def group = 'path'
        def project = 'AProject'
        ScheduledExecution job = new ScheduledExecution(
                uuid: UUID.randomUUID().toString(),
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
        e1.save()

        ReferencedExecution refexec = new ReferencedExecution(status: 'running',jobUuid: job.uuid, execution: e1)
        refexec.save()


        when:
        params.includeJobRef = true
        params.jobIdFilter = job.id
        def result = controller.index_old()


        then:
        response.status == 200
        result
        1 * controller.metricService.withTimer(_,_,_)>> [reports: []]
        1 * controller.reportService.finishquery(_, _, _) >> { ExecQuery query,def params, Map model ->
            1 == query.execIdFilter?.size()
            model
        }
        where:
        includeJobRef   | expected
        true            | 1
        false           | null

    }
}

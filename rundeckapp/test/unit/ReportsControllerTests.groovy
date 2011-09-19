import grails.test.ControllerUnitTestCase
import com.dtolabs.rundeck.core.common.Framework
/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/*
 * ReportsControllerTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 9/19/11 8:44 AM
 * 
 */
class ReportsControllerTests extends ControllerUnitTestCase {

    public void testAuthorizedJobReportIsIncluded() {
        mockDomain(ScheduledExecution)
        mockDomain(BaseReport)
        mockDomain(ExecReport)


        //create test job
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath: 'some/where', description: 'a job', project: 'testproject', argString: '-a b -c d', adhocExecution: false)

        se.save()

        //test 1 report

        ExecReport rpt1 = new ExecReport(jcJobId: se.id.toString(),actionType: 'succeed',ctxProject: 'testproject',status: 'succeed',title:"blah", author:"bloo",dateStarted: new Date(),dateCompleted: new Date())
        rpt1.save()

        def svcControl = mockFor(ReportService, true)
        svcControl.demand.getCombinedReports {query-> return [reports:[rpt1],lastDate:1l] }
        svcControl.demand.finishquery {query,params,model -> return model }

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }


        def secControl= mockFor(ScheduledExecutionService, false)
        secControl.demand.userAuthorizedForJobAction {request, sei, framework, action->
            assertEquals(UserAuth.EV_READ, action)
            return true
        }

        def userControl= mockFor(UserService, false)
        userControl.demand.findOrCreateUser{user->[]}
        userControl.demand.parseKeyValuePref{pref->[:]}


        ReportsController ctrl = new ReportsController()
        ctrl.reportService = svcControl.createMock()
        ctrl.frameworkService = fwkControl.createMock()
        ctrl.scheduledExecutionService = secControl.createMock()
        ctrl.userService = userControl.createMock()

        ReportQuery query = new ReportQuery()
        query.projFilter="testproject"


        FrameworkController.metaClass.static.'autosetSessionProject'={ session, framework->}

        def model=ctrl.index(query)
        assertNotNull(model)
        assertNotNull(model.reports)
        assertEquals(1,model.reports.size())
    }

    public void testUnauthorizedJobReportIsNotIncluded() {
        mockDomain(ScheduledExecution)
        mockDomain(BaseReport)
        mockDomain(ExecReport)

        //create test job
        ScheduledExecution se = new ScheduledExecution(jobName: 'blue', groupPath: 'some/where', description: 'a job', project: 'testproject', argString: '-a b -c d', adhocExecution: false)

        se.save()

        //test 1 report

        ExecReport rpt1 = new ExecReport(jcJobId: se.id.toString(), actionType: 'succeed', ctxProject: 'testproject', status: 'succeed', title: "blah", author: "bloo", dateStarted: new Date(), dateCompleted: new Date())
        rpt1.save()

        def svcControl = mockFor(ReportService, true)
        svcControl.demand.getCombinedReports {query -> return [reports: [rpt1], lastDate: 1l] }
        svcControl.demand.finishquery {query, params, model -> return model }

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }


        def secControl = mockFor(ScheduledExecutionService, false)
        secControl.demand.userAuthorizedForJobAction {request, sei, framework, action ->
            assertEquals(UserAuth.EV_READ, action)
            return false
        }

        def userControl = mockFor(UserService, false)
        userControl.demand.findOrCreateUser {user -> []}
        userControl.demand.parseKeyValuePref {pref -> [:]}


        ReportsController ctrl = new ReportsController()
        ctrl.reportService = svcControl.createMock()
        ctrl.frameworkService = fwkControl.createMock()
        ctrl.scheduledExecutionService = secControl.createMock()
        ctrl.userService = userControl.createMock()

        ReportQuery query = new ReportQuery()
        query.projFilter = "testproject"


        FrameworkController.metaClass.static.'autosetSessionProject' = { session, framework ->}

        def model = ctrl.index(query)
        assertNotNull(model)
        assertNotNull(model.reports)
        assertEquals(0, model.reports.size())
    }


    public void testAuthorizedAdhocReportIsIncluded() {
        mockDomain(ScheduledExecution)
        mockDomain(BaseReport)
        mockDomain(ExecReport)

        //test 1 report

        ExecReport rpt1 = new ExecReport(adhocExecution: true, actionType: 'succeed', ctxProject: 'testproject', status: 'succeed', title: "blah", author: "bloo", dateStarted: new Date(), dateCompleted: new Date())
        rpt1.save()

        def svcControl = mockFor(ReportService, true)
        svcControl.demand.getCombinedReports {query -> return [reports: [rpt1], lastDate: 1l] }
        svcControl.demand.finishquery {query, params, model -> return model }

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }


        def secControl = mockFor(ScheduledExecutionService, false)
        secControl.demand.userAuthorizedForAdhocAction {request, project, framework, action ->
            assertEquals(UserAuth.EV_READ,action)
            return true
        }

        def userControl = mockFor(UserService, false)
        userControl.demand.findOrCreateUser {user -> []}
        userControl.demand.parseKeyValuePref {pref -> [:]}


        ReportsController ctrl = new ReportsController()
        ctrl.reportService = svcControl.createMock()
        ctrl.frameworkService = fwkControl.createMock()
        ctrl.scheduledExecutionService = secControl.createMock()
        ctrl.userService = userControl.createMock()

        ReportQuery query = new ReportQuery()
        query.projFilter = "testproject"


        FrameworkController.metaClass.static.'autosetSessionProject' = { session, framework ->}

        def model = ctrl.index(query)
        assertNotNull(model)
        assertNotNull(model.reports)
        assertEquals(1, model.reports.size())
    }

    public void testUnauthorizedAdhocReportIsNotIncluded() {
        mockDomain(ScheduledExecution)
        mockDomain(BaseReport)
        mockDomain(ExecReport)

        //test 1 report

        ExecReport rpt1 = new ExecReport(adhocExecution: true, actionType: 'succeed', ctxProject: 'testproject', status: 'succeed', title: "blah", author: "bloo", dateStarted: new Date(), dateCompleted: new Date())
        rpt1.save()

        def svcControl = mockFor(ReportService, true)
        svcControl.demand.getCombinedReports {query -> return [reports: [rpt1], lastDate: 1l] }
        svcControl.demand.finishquery {query, params, model -> return model }

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }


        def secControl = mockFor(ScheduledExecutionService, false)
        secControl.demand.userAuthorizedForAdhocAction {request, project, framework, action ->
            assertEquals(UserAuth.EV_READ, action)
            return false
        }

        def userControl = mockFor(UserService, false)
        userControl.demand.findOrCreateUser {user -> []}
        userControl.demand.parseKeyValuePref {pref -> [:]}


        ReportsController ctrl = new ReportsController()
        ctrl.reportService = svcControl.createMock()
        ctrl.frameworkService = fwkControl.createMock()
        ctrl.scheduledExecutionService = secControl.createMock()
        ctrl.userService = userControl.createMock()

        ReportQuery query = new ReportQuery()
        query.projFilter = "testproject"


        FrameworkController.metaClass.static.'autosetSessionProject' = { session, framework ->}

        def model = ctrl.index(query)
        assertNotNull(model)
        assertNotNull(model.reports)
        assertEquals(0, model.reports.size())
    }
}

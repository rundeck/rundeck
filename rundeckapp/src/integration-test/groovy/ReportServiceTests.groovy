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

import com.dtolabs.rundeck.app.support.ExecQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.junit.Test
import org.rundeck.app.data.providers.v1.report.ExecReportDataProvider
import rundeck.ExecReport
import rundeck.BaseReport
import rundeck.ScheduledExecution
import rundeck.services.ReportService

/*
 * ReportServiceTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 8/23/12 2:57 PM
 * 
 */
@Integration
@Rollback
class ReportServiceTests extends GroovyTestCase {
    ReportService reportService
    ExecReportDataProvider execReportDataProvider
    def sessionFactory

    private BaseReport proto(props=[:]){
        def repprops=[author: 'bob', project: 'proj1', status: 'succeed', actionType: 'succeed', dateCompleted: new Date(), dateStarted:new Date(),
        message:'message',title:'title']
        return new ExecReport(repprops+props)
    }
    @Test
    void testGetExecReportsReportIdFilter(){
        def r1, r2, r3

        r1 = proto(reportId: 'blah', executionId: '123', executionUuid: 'uuid1')
        assert r1.validate()
        assert null != r1.save(flush: true)
        assert 'blah' == r1.reportId
        assertNotNull(r1.id)
        r2 = proto(reportId: 'blah2', executionId: '124', executionUuid: 'uuid2')
        assert r2.validate()
        assert null != r2.save(flush: true)
        r3 = proto(reportId: 'blah3', executionId: '125', executionUuid: 'uuid3')
        assert r3.validate()
        println r3.save(flush: true)

        r1 = r1.refresh()
        r2 = r2.refresh()
        r3 = r3.refresh()
        assertEquals(3, ExecReport.count())
        def query = new ExecQuery(reportIdFilter: 'blah')

        def result = reportService.getExecutionReports(query, true)
        assert result.total == 1
        assert result.reports.size() == 1
        assert result.reports.contains(r1)

        assertQueryResult([reportIdFilter: 'blah'], [r1])
        assertQueryResult([reportIdFilter: 'blah2'], [r2])
        assertQueryResult([reportIdFilter: 'blah3'], [r3])
        assertQueryResult([reportIdFilter: 'blah4'], [])
    }
    @Test
    void testGetExecNodeFilterReportIdFilter(){
        def r1,r2,r3, r4

            r1=proto(reportId:'blah', executionId: '123', succeededNodeList:'test', executionUuid: 'uuid1')
            assert r1.validate()
            assert null!=r1.save(flush: true)
            assert 'blah'==r1.reportId
            assertNotNull(r1.id)
            r2 = proto(reportId: 'blah2', executionId: '124', failedNodeList:'test', executionUuid: 'uuid2')
            assert r2.validate()
            assert null != r2.save(flush: true)
            r3 = proto(reportId: 'blah3', executionId: '125', filterApplied:'tags: monkey', executionUuid: 'uuid3')
            assert r3.validate()
            println r3.save(flush: true)
            r4 = proto(reportId: 'blah4', executionId: '126', filterApplied:'.*',succeededNodeList:'test', executionUuid: 'uuid4')
            assert r4.validate()
            println r4.save(flush: true)

            sessionFactory.currentSession.flush()

        r1=r1.refresh()
        r2=r2.refresh()
        r3=r3.refresh()
        r4=r4.refresh()
        assertEquals(4,ExecReport.count())
        def query = new ExecQuery(execnodeFilter: 'name: test')

        def result=reportService.getExecutionReports(query,true)
        assert result.total==3
        assert result.reports.size()==3
        assert result.reports.contains(r1)
        assert result._filters.containsKey('execnode')

        assertQueryResult([execnodeFilter: 'name: test'], [r1,r2,r4])
        assertQueryResult([execnodeFilter: 'test'], [r1,r2,r4])
        assertQueryResult([execnodeFilter: 'tags: monkey'], [r3])
        assertQueryResult([execnodeFilter: 'tags: test'], [])
        assertQueryResult([execnodeFilter: '.*'], [r4])

    }

    @Test
    void testGetExecReportsProjFilterIsExact(){
        def r1,r2,r3

        r1=proto(reportId:'blah', executionId: '123', project:'abc', executionUuid: 'uuid1')
        assert r1.validate()
        assert null!=r1.save(flush: true)
        assert 'blah'==r1.reportId
        assertNotNull(r1.id)
        r2 = proto(reportId: 'blah2', executionId: '124', project: 'abc', executionUuid: 'uuid2')
        assert r2.validate()
        assert null != r2.save(flush: true)
        r3 = proto(reportId: 'blah3', executionId: '125', project: 'abcdef', executionUuid: 'uuid3')
        assert r3.validate()
        println r3.save(flush: true)

        sessionFactory.currentSession.flush()

        r1=r1.refresh()
        r2=r2.refresh()
        r3=r3.refresh()
        assertEquals(3,ExecReport.count())
        def query = new ExecQuery(projFilter: 'abcdef')

        def result=reportService.getExecutionReports(query,true)
        assert result.total==1
        assert result.reports.size()==1
        assert result.reports.contains(r3)

        assertQueryResult([projFilter: 'abc'], [r1,r2])
        assertQueryResult([projFilter: 'abcd'], [])
        assertQueryResult([projFilter: 'abcdef'], [r3])
    }
    @Test
    void testGetExecReportsJobListFilter(){
        def r1 = proto(reportId: 'group/name', executionId: '1', executionUuid: 'uuid1')
        assert null != r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', executionId: '2', executionUuid: 'uuid2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3', executionId: '3', executionUuid: 'uuid3')
        assert null != r3.save(flush: true)
        def r4 = proto(reportId: 'group/monkey', executionId: '4', executionUuid: 'uuid4')
        assert null != r4.save(flush: true)

        assertQueryResult([jobListFilter: ['group/name']], [r1])
        assertQueryResult([jobListFilter: ['group/name', 'group/name2']], [r1, r2])
        assertQueryResult([jobListFilter: ['group/name', 'group/name3']], [r1, r3])
        assertQueryResult([jobListFilter: ['group/name2', 'group/name3']], [r2, r3])
        assertQueryResult([jobListFilter: ['group/name', 'group/name2', 'group/name3']], [r1, r2, r3])
    }
    @Test
    void testGetExecReportsStatusStringVariations(){
        def r1 = proto(reportId: 'group/name', executionId: '1', status: 'failed', actionType: 'failed', executionUuid: 'uuid1')
        assert null != r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', executionId: '2', status: 'fail', actionType: 'fail', executionUuid: 'uuid2')
        assert null != r2.save(flush: true)

        def r3 = proto(reportId: 'group/name2', executionId: '3', status: 'succeed', actionType: 'succeed', executionUuid: 'uuid3')
        assert null != r3.save(flush: true)
        def r4 = proto(reportId: 'group/name2', executionId: '4', status: 'succeeded', actionType: 'succeeded', executionUuid: 'uuid4')
        assert null != r4.save(flush: true)

        assertQueryResult([statFilter: 'fail'], [r1, r2])
        assertQueryResult([statFilter: 'succeed'], [r3, r4])

    }
    @Test
    void testGetCombinedReportsExcludeJobListFilter(){
        def r1 = proto(reportId: 'group/name', executionId: '1', executionUuid: 'uuid1')
        assert null != r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', executionId: '2', executionUuid: 'uuid2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3', executionId: '3', executionUuid: 'uuid3')
        assert null != r3.save(flush: true)

        assertQueryResult([excludeJobListFilter: ['group/name']], [r2, r3])
        assertQueryResult([excludeJobListFilter: ['group/name2']], [r1, r3])
        assertQueryResult([excludeJobListFilter: ['group/name3']], [r1, r2])
        assertQueryResult([excludeJobListFilter: ['group/name', 'group/name2']], [r3])
        assertQueryResult([excludeJobListFilter: ['group/name', 'group/name3']], [r2])
        assertQueryResult([excludeJobListFilter: ['group/name', 'group/name2', 'group/name3']], [])
    }

    @Test
    void testConvertUUIDJobIdFilterToInternalId(){
        String seUUID = UUID.randomUUID().toString()
        String seUUID2 = UUID.randomUUID().toString()
        def se = new ScheduledExecution(project:"one",jobName: "TestJob",uuid: seUUID)
        def se2 = new ScheduledExecution(project:"one",jobName: "TestJob2",uuid: seUUID2)
        assert null != se.save(flush: true)
        assert null != se2.save(flush: true)

        def r1 = proto(reportId: 'group/name', jobId: se.id,project: 'one', executionUuid: 'uuid1')
        assert null != r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', jobId: se2.id,project: 'one', executionUuid: 'uuid2')
        assert null != r2.save(flush: true)
        sessionFactory.currentSession.flush()

        def pquery = new ExecQuery(projFilter: 'one')
        def pres =reportService.getExecutionReports(pquery,true)
        assert pres.total == 2

        def query = new ExecQuery(projFilter: 'one',jobIdFilter: seUUID)
        def result=reportService.getExecutionReports(query,true)

        assert result.total == 1
        result.reports.containsAll([r1])
    }

    private assertQueryResult(Map props, Collection<BaseReport> results,Integer total=null) {
        def query = new ExecQuery(props)
        def result = reportService.getExecutionReports(query, true)
        assert result.total == (null != total ? total : results.size())
        assert result.reports.size() == results.size()
        assert result.reports.containsAll(results)
    }

    @Test
    void testGetExecReportsFailedStat(){
        def r1,r2,r3

        r1 = proto(reportId: 'blah', executionId: '123', status: 'fail', executionUuid: 'uuid1')
        assert r1.validate()
        assert null != r1.save(flush: true)
        assert 'blah' == r1.reportId
        assertNotNull(r1.id)
        r2 = proto(reportId: 'blah2', executionId: '124', status: 'fail', executionUuid: 'uuid2')
        assert r2.validate()
        assert null != r2.save(flush: true)
        r3 = proto(reportId: 'blah3', executionId: '125', status: 'succes', executionUuid: 'uuid3')
        assert r3.validate()
        println r3.save(flush: true)

        sessionFactory.currentSession.flush()

        r1 = r1.refresh()
        r2 = r2.refresh()
        r3 = r3.refresh()
        assertEquals(3, ExecReport.count())
        def query = new ExecQuery(statFilter: 'fail')

        def result = reportService.getExecutionReports(query, true)
        assert result.total == 2
        assert result.reports.size() == 2
        assert result.reports.contains(r1)
        assert result.reports.contains(r2)
        assert !result.reports.contains(r3)

    }

    @Test
    void testGetExecReportsKilledStat(){
        def r1,r2,r3

        r1=proto(reportId:'blah', executionId: '123', status: 'fail', abortedByUser: 'admin', executionUuid: 'uuid1')
        assert r1.validate()
        assert null!=r1.save(flush: true)
        assert 'blah'==r1.reportId
        assertNotNull(r1.id)
        r2 = proto(reportId: 'blah2', executionId: '124',status: 'fail', executionUuid: 'uuid2')
        assert r2.validate()
        assert null != r2.save(flush: true)
        r3 = proto(reportId: 'blah3', executionId: '125',status: 'succes', executionUuid: 'uuid3')
        assert r3.validate()
        println r3.save(flush: true)

        sessionFactory.currentSession.flush()

        r1=r1.refresh()
        r2=r2.refresh()
        r3=r3.refresh()
        assertEquals(3,ExecReport.count())
        def query = new ExecQuery(statFilter: 'cancel')

        def result=reportService.getExecutionReports(query,true)
        assert result.total==1
        assert result.reports.size()==1
        assert result.reports.contains(r1)
        assert !result.reports.contains(r2)
        assert !result.reports.contains(r3)

    }
}

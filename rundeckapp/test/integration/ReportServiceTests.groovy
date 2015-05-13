import com.dtolabs.rundeck.app.support.ExecQuery
import rundeck.ExecReport
import rundeck.BaseReport
import rundeck.services.ReportService

/*
* Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
*
*/
 
/*
 * ReportServiceTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 8/23/12 2:57 PM
 * 
 */
class ReportServiceTests extends GroovyTestCase {
    def ReportService reportService
    def sessionFactory

    private BaseReport proto(props=[:]){
        def repprops=[author: 'bob', ctxProject: 'proj1', status: 'succeed', actionType: 'succeed', dateCompleted: new Date(), dateStarted:new Date(),
        message:'message',title:'title']
        return new ExecReport(repprops+props)
    }
    void testgetExecReportsReportIdFilter(){
        def r1,r2,r3

        ExecReport.withNewSession {
            r1=proto(reportId:'blah', jcExecId: '123')
            assert r1.validate()
            assert null!=r1.save(flush: true)
            assert 'blah'==r1.reportId
            assertNotNull(r1.id)
            r2 = proto(reportId: 'blah2', jcExecId: '124')
            assert r2.validate()
            assert null != r2.save(flush: true)
            r3 = proto(reportId: 'blah3', jcExecId: '125')
            assert r3.validate()
            println r3.save(flush: true)

            sessionFactory.currentSession.flush()
        }
        r1=r1.refresh()
        r2=r2.refresh()
        r3=r3.refresh()
        assertEquals(3,ExecReport.count())
        def query = new ExecQuery(reportIdFilter: 'blah')

        def result=reportService.getExecutionReports(query,true)
        assert result.total==1
        assert result.reports.size()==1
        assert result.reports.contains(r1)

        assertQueryResult([reportIdFilter: 'blah'], [r1])
        assertQueryResult([reportIdFilter: 'blah2'], [r2])
        assertQueryResult([reportIdFilter: 'blah3'], [r3])
        assertQueryResult([reportIdFilter: 'blah4'], [])
    }
    void testgetExecReportsProjFilterIsExact(){
        def r1,r2,r3

        ExecReport.withNewSession {
            r1=proto(reportId:'blah', jcExecId: '123', ctxProject:'abc')
            assert r1.validate()
            assert null!=r1.save(flush: true)
            assert 'blah'==r1.reportId
            assertNotNull(r1.id)
            r2 = proto(reportId: 'blah2', jcExecId: '124', ctxProject: 'abc')
            assert r2.validate()
            assert null != r2.save(flush: true)
            r3 = proto(reportId: 'blah3', jcExecId: '125', ctxProject: 'abcdef')
            assert r3.validate()
            println r3.save(flush: true)

            sessionFactory.currentSession.flush()
        }
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
    void testgetExecReportsJobListFilter(){
        def r1=proto(reportId:'group/name',jcExecId:'1')
        assert null!=r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', jcExecId: '2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3', jcExecId: '3')
        assert null != r3.save(flush: true)
        def r4 = proto(reportId: 'group/monkey', jcExecId: '4')
        assert null != r4.save(flush: true)

        assertQueryResult([jobListFilter: ['group/name']],[r1])
        assertQueryResult([jobListFilter: ['group/name','group/name2']],[r1,r2])
        assertQueryResult([jobListFilter: ['group/name','group/name3']],[r1,r3])
        assertQueryResult([jobListFilter: ['group/name2','group/name3']],[r2,r3])
        assertQueryResult([jobListFilter: ['group/name','group/name2','group/name3']],[r1,r2,r3])
    }
    void testgetExecReportsStatusStringVariations(){
        def r1=proto(reportId:'group/name',jcExecId:'1',status:'failed',actionType: 'failed')
        assert null!=r1.save(flush: true)
        def r2=proto(reportId:'group/name2',jcExecId:'2',status:'fail',actionType: 'fail')
        assert null!=r2.save(flush: true)

        def r3=proto(reportId:'group/name2',jcExecId:'3',status:'succeed',actionType: 'succeed')
        assert null!=r3.save(flush: true)
        def r4=proto(reportId:'group/name2',jcExecId:'4',status:'succeeded',actionType: 'succeeded')
        assert null!=r4.save(flush: true)

        assertQueryResult([statFilter: 'fail'],[r1,r2])
        assertQueryResult([statFilter: 'succeed'],[r3,r4])

    }
    void testgetCombinedReportsExcludeJobListFilter(){
        def r1=proto(reportId:'group/name', jcExecId: '1')
        assert null!=r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2', jcExecId: '2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3', jcExecId: '3')
        assert null != r3.save(flush: true)

        assertQueryResult([excludeJobListFilter: ['group/name']],[r2,r3])
        assertQueryResult([excludeJobListFilter: ['group/name2']],[r1,r3])
        assertQueryResult([excludeJobListFilter: ['group/name3']],[r1,r2])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name2']],[r3])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name3']],[r2])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name2','group/name3']],[])
    }

    private assertQueryResult(Map props, Collection<BaseReport> results,Integer total=null) {
        def query = new ExecQuery(props)

        def result = reportService.getExecutionReports(query,true)
        assert result.total == (null != total ? total : results.size())
        assert result.reports.size() == results.size()
        assert result.reports.containsAll(results)
    }
}

import grails.test.GrailsUnitTestCase
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

    private BaseReport proto(props=[:]){
        def repprops=[author: 'bob', ctxProject: 'proj1', status: 'succeed', actionType: 'succeed', dateCompleted: new Date(), dateStarted:new Date(),
        message:'message',title:'title']
        return new ExecReport(repprops+props)
    }
    void testgetCombinedReportsReportIdFilter(){
        def r1=proto(reportId:'blah')
        assert null!=r1.save(flush: true)
        def r2 = proto(reportId: 'blah2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'blah3')
        assert null != r3.save(flush: true)

        def query = new ReportQuery(reportIdFilter: 'blah')

        def result=reportService.getCombinedReports(query)
        assert result.total==1
        assert result.reports.size()==1
        assert result.reports.contains(r1)

        assertQueryResult([reportIdFilter: 'blah'], [r1])
        assertQueryResult([reportIdFilter: 'blah2'], [r2])
        assertQueryResult([reportIdFilter: 'blah3'], [r3])
        assertQueryResult([reportIdFilter: 'blah4'], [])
    }
    void testgetCombinedReportsJobListFilter(){
        def r1=proto(reportId:'group/name')
        assert null!=r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3')
        assert null != r3.save(flush: true)
        def r4 = proto(reportId: 'group/monkey')
        assert null != r4.save(flush: true)

        assertQueryResult([jobListFilter: ['group/name']],[r1])
        assertQueryResult([jobListFilter: ['group/name','group/name2']],[r1,r2])
        assertQueryResult([jobListFilter: ['group/name','group/name3']],[r1,r3])
        assertQueryResult([jobListFilter: ['group/name2','group/name3']],[r2,r3])
        assertQueryResult([jobListFilter: ['group/name','group/name2','group/name3']],[r1,r2,r3])
    }
    void testgetCombinedReportsExcludeJobListFilter(){
        def r1=proto(reportId:'group/name')
        assert null!=r1.save(flush: true)
        def r2 = proto(reportId: 'group/name2')
        assert null != r2.save(flush: true)
        def r3 = proto(reportId: 'group/name3')
        assert null != r3.save(flush: true)

        assertQueryResult([excludeJobListFilter: ['group/name']],[r2,r3])
        assertQueryResult([excludeJobListFilter: ['group/name2']],[r1,r3])
        assertQueryResult([excludeJobListFilter: ['group/name3']],[r1,r2])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name2']],[r3])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name3']],[r2])
        assertQueryResult([excludeJobListFilter: ['group/name','group/name2','group/name3']],[])
    }

    private assertQueryResult(Map props, Collection<BaseReport> results,Integer total=null) {
        def query = new ReportQuery(props)

        def result = reportService.getCombinedReports(query)
        assert result.total == (null != total ? total : results.size())
        assert result.reports.size() == results.size()
        assert result.reports.containsAll(results)
    }
}

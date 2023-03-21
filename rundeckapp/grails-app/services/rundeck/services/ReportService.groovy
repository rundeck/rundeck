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

package rundeck.services

import com.dtolabs.rundeck.app.support.ExecQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import com.google.common.collect.Lists
import grails.gorm.DetachedCriteria
import grails.gorm.transactions.Transactional
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.app.data.model.v1.query.RdExecQuery
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequestImpl
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponseImpl
import org.rundeck.app.data.providers.v1.ExecReportDataProvider
import org.rundeck.core.auth.AuthConstants
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution

import javax.sql.DataSource

@Transactional
class ReportService  {

    def grailsApplication
    AppAuthContextEvaluator rundeckAuthContextEvaluator
    ConfigurationService configurationService
    ExecReportDataProvider execReportDataProvider

    static final String GRANTED_VIEW_HISTORY_JOBS = "granted_view_history_jobs"
    static final String DENIED_VIEW_HISTORY_JOBS = "rejected_view_history_jobs"

    public Map reportExecutionResult(SaveReportRequestImpl saveReportRequest) {
        /**
         * allowed fields are specified
         */
        if (!saveReportRequest.dateStarted) {
            saveReportRequest.dateStarted= saveReportRequest.dateCompleted
        }
        if(!saveReportRequest.message){
            saveReportRequest.message="[no message]"
        }
        SaveReportResponseImpl saveReportResponse = execReportDataProvider.saveReport(saveReportRequest)

        //TODO: authorize event creation?

        if (saveReportResponse.report && !saveReportResponse.isSaved) {
//            System.err.println("error saving report: ${fields}")
//            rep.errors.allErrors.each {
//                System.err.println(it)
//            }
            return [error:true,report:saveReportResponse.report]
        }else{
            return [success:true]
        }
    }

     def public finishquery(ExecQuery query,def params, Map model){

        if(!params.max){
            params.max=grailsApplication.config.getProperty("reportservice.pagination.default",Integer.class, 20)
        }
        if(!params.offset){
            params.offset=0
        }

        def paginateParams=[:]
        if(query){
            model._filters.each{ key,val ->
                if(query."${key}Filter"){
                    paginateParams["${key}Filter"]=query."${key}Filter"
                }
            }
        }
        def displayParams = [:]
        displayParams.putAll(paginateParams)

//        params.each{key,val ->
//            if(key ==~ /^(start|end)(do|set|before|after)Filter.*/ && query[key]){
//                paginateParams[key]=val
//            }
//        }
        if(query){
            if(query.recentFilter && query.recentFilter!='-'){
                displayParams.put("recentFilter",query.recentFilter)
                paginateParams.put("recentFilter",query.recentFilter)
                query.dostartafterFilter=false
                query.dostartbeforeFilter=false
                query.doendafterFilter=false
                query.doendbeforeFilter=false
            }else{
                if(query.dostartafterFilter && query.startafterFilter){
                    displayParams.put("startafterFilter",query.startafterFilter)
                    paginateParams.put("startafterFilter",query.startafterFilter)
                    paginateParams.put("dostartafterFilter","true")
                }
                if(query.dostartbeforeFilter && query.startbeforeFilter){
                    displayParams.put("startbeforeFilter",query.startbeforeFilter)
                    paginateParams.put("startbeforeFilter",query.startbeforeFilter)
                    paginateParams.put("dostartbeforeFilter","true")
                }
                if(query.doendafterFilter && query.endafterFilter){
                    displayParams.put("endafterFilter",query.endafterFilter)
                    paginateParams.put("endafterFilter",query.endafterFilter)
                    paginateParams.put("doendafterFilter","true")
                }
                if(query.doendbeforeFilter && query.endbeforeFilter){
                    displayParams.put("endbeforeFilter",query.endbeforeFilter)
                    paginateParams.put("endbeforeFilter",query.endbeforeFilter)
                    paginateParams.put("doendbeforeFilter","true")
                }
            }
        }
        def remkeys=[]
        if(!query || !query.dostartafterFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^startafterFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.dostartbeforeFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^startbeforeFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.doendafterFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^endafterFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        if(!query || !query.doendbeforeFilter){
            paginateParams.each{key,val ->
                if(key ==~ /^endbeforeFilter.*/){
                    remkeys.add(key)
                }
            }
        }
        remkeys.each{
            paginateParams.remove(it)
        }

         Integer defaultMax = grailsApplication.config.getProperty("reportservice.pagination.default",Integer.class, 20)

         def tmod=[max: query?.max?query.max:defaultMax,
            offset:query?.offset?query.offset:0,
            paginateParams:paginateParams,
            displayParams:displayParams]
        model.putAll(tmod)
        return model
    }

    private def getStartsWithFilters() {
        return [
            //job filter repurposed for reportId
            job: 'reportId',
        ]
    }
    private def getTxtFilters() {
        def txtfilters = [
            obj: 'ctxName',
            user: 'author',
            abortedBy: 'abortedByUser',
            node: 'node',
            message: 'message',
            title: 'title',
            tags: 'tags',
        ]
        return txtfilters
    }

    private def getEqFilters() {
        def eqfilters = [
            stat: 'status',
            reportId: 'reportId',
            jobId:'jobId',
            proj: 'project',
        ]
        return eqfilters
    }

    /**
     * Count the query results matching the filter
     */
    def countExecutionReports(RdExecQuery query) {

        def total = execReportDataProvider.countExecutionReports(query)

        return total
    }

    def applyExecutionCriteria(ExecQuery query, delegate, boolean isJobs=true, ScheduledExecution se=null){
        def eqfilters = [
                stat: 'status',
                reportId: 'reportId',
        ]
        def jobfilters = [
                jobId: 'jobId',
                proj: 'project',
        ]
        def txtfilters = [
                user: 'author',
                node: 'node',
                message: 'message',
                job: 'reportId',
                title: 'title',
                tags: 'tags',
        ]

        //in cancel case the real stat is failed but AbortedByUser != null
        boolean fixCancel = (query.statFilter=='cancel' && !query.abortedByFilter)

        delegate.with {

            if (query) {
                txtfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        ilike(val, '%' + query["${key}Filter"] + '%')
                    }
                }

                if(fixCancel){
                    query.statFilter='fail'
                    isNotNull('abortedByUser')
                }else if(query.statFilter=='fail' && !query.abortedByFilter){
                    isNull('abortedByUser')
                }

                eqfilters.each { key, val ->
                    if (query["${key}Filter"] == 'null') {
                        or{
                            isNull(val)
                            eq(val,'')
                        }
                    } else if (query["${key}Filter"] == '!null') {
                        and {
                            isNotNull(val)
                            if(!isOracleDatasource())
                                ne(val,'')
                        }
                    } else if (key=='stat' && query["${key}Filter"]=='succeed') {
                        or{
                            eq(val,'succeed')
                            eq(val,'succeeded')
                            eq(val,'true')
                        }
                    }  else if (key=='stat' && query["${key}Filter"] =='fail') {
                        or{
                            eq(val, 'fail')
                            eq(val, 'failed')
                        }
                    } else if (query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }

                if (query.execProjects && se) {
                    or {
                        exists(new DetachedCriteria(ReferencedExecution, "re").build {
                            projections { property 're.execution.id' }
                            eq('re.scheduledExecution.id', se.id)
                            eqProperty('re.execution.id', 'this.executionId')
                            List execProjectsPartitioned = Lists.partition(query.execProjects, 1000)
                            or{
                                for(def partition : execProjectsPartitioned){
                                    'in'('this.project', partition)
                                }
                            }
                        })
                        eq('jobId', String.valueOf(se.id))
                        and{
                            jobfilters.each { key, val ->
                                if (query["${key}Filter"] == 'null') {
                                    or {
                                        isNull(val)
                                        eq(val, '')
                                    }
                                } else if (query["${key}Filter"] == '!null') {
                                    and {
                                        isNotNull(val)
                                        if(!isOracleDatasource())
                                            ne(val,'')
                                    }
                                } else if (query["${key}Filter"]) {
                                    eq(val, query["${key}Filter"])
                                }
                            }
                        }
                    }
                }else{
                    jobfilters.each { key, val ->
                        if (query["${key}Filter"] == 'null') {
                            or {
                                isNull(val)
                                eq(val, '')
                            }
                        } else if (query["${key}Filter"] == '!null') {
                            and {
                                isNotNull(val)
                                if(!isOracleDatasource())
                                    ne(val,'')
                            }
                        } else if (query["${key}Filter"]) {
                            eq(val, query["${key}Filter"])
                        }
                    }
                }

                if (query.titleFilter) {
                    or {
                        eq('jobId', '')
                        isNull('jobId')
                    }
                }
                if (query.jobListFilter || query.excludeJobListFilter) {
                    and {
                        if (query.jobListFilter) {
                            or {
                                query.jobListFilter.each {
                                    eq('reportId', it)
                                }
                            }
                        }
                        if (query.excludeJobListFilter) {
                            not {
                                or {
                                    query.excludeJobListFilter.each {
                                        eq('reportId', it)
                                    }

                                }
                            }
                        }
                    }
                }

                if (query.dostartafterFilter && query.dostartbeforeFilter && query.startbeforeFilter && query.startafterFilter) {
                    between('dateStarted', query.startafterFilter, query.startbeforeFilter)
                } else if (query.dostartbeforeFilter && query.startbeforeFilter) {
                    le('dateStarted', query.startbeforeFilter)
                } else if (query.dostartafterFilter && query.startafterFilter) {
                    ge('dateStarted', query.startafterFilter)
                }

                if (query.doendafterFilter && query.doendbeforeFilter && query.endafterFilter && query.endbeforeFilter) {
                    between('dateCompleted', query.endafterFilter, query.endbeforeFilter)
                } else if (query.doendbeforeFilter && query.endbeforeFilter) {
                    le('dateCompleted', query.endbeforeFilter)
                }
                if (query.doendafterFilter && query.endafterFilter) {
                    ge('dateCompleted', query.endafterFilter)
                }
            }

            if (isJobs) {
                or {
                    isNotNull("jobId")
                    isNotNull("executionId")
                }
            } else {
                isNull("jobId")
                isNull("executionId")
            }

            if(query.execnodeFilter){
                if(query.execnodeFilter.startsWith('name:') || !(query.execnodeFilter.contains(":") || query.execnodeFilter.contains(".*"))){
                    def node = query.execnodeFilter.startsWith('name:')?(query.execnodeFilter.split("name:")[1]).stripIndent():query.execnodeFilter;
                    or {
                        ilike("failedNodeList", '%' + node + '%')
                        ilike("succeededNodeList", '%' + node + '%')
                    }

                }else{
                    ilike("filterApplied", '%' + query.execnodeFilter + '%')
                }

            }
        }
        if(fixCancel){
            query.statFilter='cancel'
        }
    }
    def getExecutionReports(RdExecQuery query, boolean isJobs) {
        def eqfilters = [
                stat: 'status',
                reportId: 'reportId',
                jobId: 'jobId',
                proj: 'project',
        ]
        def txtfilters = [
                user: 'author',
                node: 'node',
                message: 'message',
                job: 'reportId',
                title: 'title',
                tags: 'tags',
        ]
        def specialfilters = [
                execnode: 'execnode'
        ]

        def se
        if(query?.jobIdFilter) {
            se = ScheduledExecution.findByUuid(query.jobIdFilter)
            if(!se && query.jobIdFilter.isNumber()) {
                se = ScheduledExecution.get(query.jobIdFilter)
            }
            if(se) {
                query.jobIdFilter = se.id.toString()
            }
        }

        def filters = [:]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)

        def seId = se?.id?: null
        def runlist = execReportDataProvider.getExecutionReports(query, isJobs, seId)

        def executions=[]
        def lastDate = -1
        runlist.each{
            executions<<it
            if (it.dateCompleted.time > lastDate) {
                lastDate = it.dateCompleted.time
            }
        }
        def total = execReportDataProvider.countExecutionReportsWithTransaction(query, isJobs, seId)

        filters.putAll(specialfilters)

        return [
            query:query,
            reports:executions,
            total: total,
            lastDate: lastDate,
            _filters:filters
            ]
	}

    /**
     * Sorts jobs according to user permission
     *
     * @param authContext
     * @param project
     * @return list with job names and sorted by authorizations
     */
    Map jobHistoryAuthorizations(AuthContext authContext, String project){
        def decisions = authorizeViewHistoryJob(authContext, project)
        Map<String, List> authorizations = [:]

        def decisionsByJob = decisions.groupBy {
            it.resource.group ? ScheduledExecution.generateFullName(it.resource.group,it.resource.name) : it.resource.name
        }

        authorizations.put(DENIED_VIEW_HISTORY_JOBS, decisionsByJob.findAll { jobFullName, decision ->
            decision.any {
                it.explain().code == Explanation.Code.REJECTED_DENIED
            } || !decision.any {
                it.authorized
            }
        }?.collect {jobFullName, decision ->
            jobFullName
        })

        return authorizations
    }

    /**
     * Get a set of decisions considering the permissions READ, VIEW and VIEW_HISTORY
     *
     * @param authContext
     * @param project
     * @return Set of decisions
     */
    private Set<Decision> authorizeViewHistoryJob(AuthContext authContext, String project){
        def jobs = ScheduledExecution.createCriteria().list{
            projections {
                property('groupPath')
                property('jobName')
                property('uuid')
            }
            eq("project", project)
        }
        HashSet resHS = new HashSet()

        jobs.each { reg ->
            Map meta = [:]
            if(reg[0]) meta.group = reg[0]
            if(reg[1]) meta.job = reg[1]
            if(reg[2]) meta.uuid = reg[2]
            resHS.add(rundeckAuthContextEvaluator.authResourceForJob(meta.job, meta.group, meta.uuid))
        }
        HashSet constraints = new HashSet()
        constraints.addAll([AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW, AuthConstants.VIEW_HISTORY])


        return rundeckAuthContextEvaluator.authorizeProjectResources(authContext,resHS, constraints, project)
    }

    def deleteByExecutionId(Long id){
        execReportDataProvider.deleteAllByExecutionId(id)
    }
    private boolean isOracleDatasource(){
        def dataSource = applicationContext.getBean('dataSource', DataSource)
        def databaseProductName = dataSource?.getConnection()?.metaData?.databaseProductName
        return (databaseProductName == 'Oracle') ? true : false
    }
}

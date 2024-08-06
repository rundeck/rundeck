package org.rundeck.app.data.providers

import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.data.model.v1.query.RdExecQuery
import org.rundeck.app.data.model.v1.report.RdExecReport
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse
import rundeck.data.report.SaveReportResponseImpl
import org.rundeck.app.data.providers.v1.report.ExecReportDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.transaction.TransactionStatus
import rundeck.BaseReport
import rundeck.ExecReport
import rundeck.services.ConfigurationService
import javax.sql.DataSource

@CompileStatic(TypeCheckingMode.SKIP)
class GormExecReportDataProvider implements ExecReportDataProvider, DBExecReportSupport {
    @Autowired
    ConfigurationService configurationService
    @Autowired
    ApplicationContext applicationContext
    @Autowired
    MessageSource messageSource

    @Override
    RdExecReport get(Long id){
        return ExecReport.get(id)
    }

    @Override
    SaveReportResponse saveReport(SaveReportRequest saveReportRequest) {
        ExecReport execReport = new ExecReport()
        execReport.executionId = saveReportRequest.executionId
        execReport.jobId = saveReportRequest.jobId
        execReport.adhocExecution = saveReportRequest.adhocExecution
        execReport.adhocScript = saveReportRequest.adhocScript
        execReport.abortedByUser = saveReportRequest.abortedByUser
        execReport.succeededNodeList = saveReportRequest.succeededNodeList
        execReport.failedNodeList = saveReportRequest.failedNodeList
        execReport.filterApplied = saveReportRequest.filterApplied
        execReport.node = saveReportRequest.node
        execReport.title = saveReportRequest.title
        execReport.status = saveReportRequest.status
        execReport.actionType = saveReportRequest.status
        execReport.project = saveReportRequest.project
        execReport.reportId = saveReportRequest.reportId
        execReport.tags = saveReportRequest.tags
        execReport.author = saveReportRequest.author
        execReport.message = saveReportRequest.message
        execReport.dateStarted = saveReportRequest.dateStarted
        execReport.dateCompleted = saveReportRequest.dateCompleted
        execReport.jobUuid = saveReportRequest.jobUuid
        execReport.executionUuid = saveReportRequest.executionUuid
        boolean isUpdated = execReport.save(flush: true)
        String errors = execReport.errors.hasErrors() ? execReport.errors.allErrors.collect {
            messageSource.getMessage(it,null) }.join(",") : null
        return new SaveReportResponseImpl(report: execReport, isSaved: isUpdated, errors: errors)
    }

    @Override
    List<RdExecReport> findAllByProject(String projectName) {
        return ExecReport.findAllByProject(projectName)
    }

    @Override
    List<RdExecReport> findAllByStatus(String status) {
        return ExecReport.findAllByStatus(status)
    }
    @Override
    List<RdExecReport> findAllByProjectAndExecutionUuidInList(String projectName, List<String> execUuids) {
        return ExecReport.findAllByProjectAndExecutionUuidInList(projectName, execUuids)
    }

    @Override
    int countByProject(String projectName) {
        return ExecReport.countByProject(projectName)
    }

    @Override
    int countExecutionReports(RdExecQuery execQuery) {
        return ExecReport.createCriteria().count {
            applyExecutionCriteria(execQuery, delegate)
        }
    }

    @Override
    int countExecutionReportsWithTransaction(RdExecQuery query, boolean isJobs, String jobId) {
        return ExecReport.withTransaction {
            ExecReport.createCriteria().count {
                applyExecutionCriteria(query, delegate, isJobs, jobId, [])
            }
        }
    }

    @Override
    int countAndSaveByStatus() {
        int count=0
        ExecReport.findAllByStatus("succeeded").each{
            it.status='succeed'
            it.actionType='succeed'
            it.save()
            count++
        }
        ExecReport.findAllByStatus("failed").each{
            it.status='fail'
            it.actionType='fail'
            it.save()
            count++
        }

        return count
    }

    @Override
    List<RdExecReport> getExecutionReports(RdExecQuery query, boolean isJobs, String jobId, List<String> execUuids) {
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
        def filters = [:]
        filters.putAll(txtfilters)
        filters.putAll(eqfilters)

        return ExecReport.createCriteria().list {

            if (query?.max) {
                maxResults(query?.max.toInteger())
            } else {
                maxResults(configurationService.getInteger("pagination.default.max",20))
            }
            if (query?.offset) {
                firstResult(query.offset.toInteger())
            }
            applyExecutionCriteria(query, delegate,isJobs, jobId, execUuids)

            if (query && query.sortBy && filters[query.sortBy]) {
                order(filters[query.sortBy], query.sortOrder == 'ascending' ? 'asc' : 'desc')
            } else {
                order("dateCompleted", 'desc')
            }
        } as List<RdExecReport>
    }

    @Override
    void deleteByProject(String projectName) {
        ExecReport.deleteByProject(projectName)
    }

    @Override
    void deleteWithTransaction(String projectName) {
        BaseReport.withTransaction { TransactionStatus status ->
            try {
                BaseReport.deleteByProject(projectName)
                ExecReport.deleteByProject(projectName)
            } catch (Exception e){
                status.setRollbackOnly()
                throw e
            }
        }
    }

    @Override
    void deleteAllByExecutionUuid(String executionUuid) {
        ExecReport.findAllByExecutionUuid(executionUuid).each { rpt ->
            rpt.delete()
        }
    }

    @Override
    void deleteAllByExecutionId(Long id) {
        ExecReport.findAllByExecutionId(id).each { rpt ->
            rpt.delete()
        }
    }

    def applyExecutionCriteria(RdExecQuery query, delegate, boolean isJobs=true, String seId=null, List<String> execUuids=[]){
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

                if (query.execProjects && seId) {
                    or {
                        if(execUuids && execUuids.size() > 0){
                            and{
                                'in'('executionUuid', execUuids)
                                List execProjectsPartitioned = Lists.partition(query.execProjects, 1000)
                                or{
                                    for(def partition : execProjectsPartitioned){
                                        'in'('project', partition)
                                    }
                                }
                            }
                        }


                        eq('jobUuid', seId)
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
                        } else if (key == "jobId" && query["${key}Filter"]) {
                            or {
                                eq(val, query["${key}Filter"])
                                if(seId) {
                                    eq('jobUuid', seId)
                                }
                            }
                        }else if (query["${key}Filter"]) {
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
    private boolean isOracleDatasource(){
        def dataSource = applicationContext.getBean('dataSource', DataSource)
        def databaseProductName = dataSource?.getConnection()?.metaData?.databaseProductName
        return (databaseProductName == 'Oracle')
    }
}

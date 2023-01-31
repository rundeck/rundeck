package org.rundeck.app.data.providers

import com.dtolabs.rundeck.app.support.ExecQuery
import com.google.common.collect.Lists
import grails.gorm.DetachedCriteria;
import org.rundeck.app.data.model.v1.report.RdExecReport
import org.rundeck.app.data.model.v1.report.dto.SaveReportResponse;
import org.rundeck.app.data.providers.v1.ExecReportDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors;
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ReferencedExecution
import rundeck.ScheduledExecution
import rundeck.services.ConfigurationService
import rundeck.services.data.ExecReportDataService;

class GormExecReportDataProvider implements ExecReportDataProvider {
    @Autowired
    ExecReportDataService execReportDataService
    @Autowired
    ConfigurationService configurationService

    @Override
    RdExecReport get(Long id){
        return ExecReport.get(id)
    }

    @Override
    RdExecReport fromExecWithScheduled(Map executionMap, Map scheduledExecutionMap) {
        ScheduledExecution scheduledExecution = ScheduledExecution.fromMap(scheduledExecutionMap)
        Execution execution = Execution.fromMap(executionMap, scheduledExecution)
        return ExecReport.fromExec(execution)
    }

    @Override
    RdExecReport fromExecWithScheduledAndSave(Map executionMap, Map scheduledExecutionMap, Long seId) {
        ScheduledExecution scheduledExecution = ScheduledExecution.fromMap(scheduledExecutionMap)
        scheduledExecution.id = seId
        Execution execution = Execution.fromMap(executionMap, scheduledExecution)
        ExecReport execReport = ExecReport.fromExec(execution)
        execReport.save()
        return execReport
    }

    @Override
    SaveReportResponse fromExecWithId(Long id) {
        Execution execution = Execution.get(id)
        ExecReport execReport = ExecReport.fromExec(execution)
        boolean isUpdated = execReport.save(flush: true)
        Errors errors = execReport.errors
        return new SaveReportResponse(report: execReport, isSaved: isUpdated, errors: errors)
    }

    @Override
    SaveReportResponse saveFromMap(Map execReportMap) {
        ExecReport execReport = ExecReport.fromMap(execReportMap)
        boolean isUpdated = execReport.save(flush: true)
        return new SaveReportResponse(report: execReport, isSaved: isUpdated)
    }

    @Override
    SaveReportResponse saveFromMapFields(Map execReportFields) {
        ExecReport execReport = new ExecReport(execReportFields)
        boolean isUpdated = execReport.save(flush: true)
        return new SaveReportResponse(report: execReport, isSaved: isUpdated)
    }

    @Override
    List<RdExecReport> findAllByStatus(String status) {
        return ExecReport.findAllByStatus(status)
    }
    @Override
    List<RdExecReport> findAllByExecutionId(Long id) {
        return ExecReport.findAllByExecutionId(id)
    }
    @Override
    List<RdExecReport> findAllByCtxProjectAndExecutionIdInList(String projectName, List<Long> execIds) {
        return ExecReport.findAllByCtxProjectAndExecutionIdInList(projectName, execIds)
    }

    @Override
    int countByCtxProject(String projectName) {
        return ExecReport.countByCtxProject(projectName)
    }

    @Override
    int countExecutionReports(Map execQueryMap) {
        ExecQuery execQuery = ExecQuery.fromMap(execQueryMap)
        return ExecReport.createCriteria().count {
            applyExecutionCriteria(execQuery, delegate)
        }
    }

    @Override
    int countExecutionReportsWithTransaction(Map query, boolean isJobs, Long scheduledExecutionId, Integer isolationLevel) {
        return ExecReport.withTransaction([isolationLevel: isolationLevel]) {
            ExecReport.createCriteria().count {
                applyExecutionCriteria(query, delegate, isJobs, scheduledExecutionId)
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
    Collection<String> getRunList(Map query, Map filters, boolean isJobs, Long scheduledExecutionId) {
         return ExecReport.createCriteria().list {

            if (query?.max) {
                maxResults(query?.max.toInteger())
            } else {
                maxResults(configurationService.getInteger("pagination.default.max",20))
            }
            if (query?.offset) {
                firstResult(query.offset.toInteger())
            }
            applyExecutionCriteria(query, delegate,isJobs, scheduledExecutionId)

            if (query && query.sortBy && filters[query.sortBy]) {
                order(filters[query.sortBy], query.sortOrder == 'ascending' ? 'asc' : 'desc')
            } else {
                order("dateCompleted", 'desc')
            }
        }
    }

    @Override
    void deleteByCtxProject(String projectName) {
        ExecReport.deleteByCtxProject(projectName)
    }

    @Override
    void deleteAllByExecutionId(Long executionId) {
        ExecReport.findAllByExecutionId(executionId).each { rpt ->
            rpt.delete()
        }
    }

    def applyExecutionCriteria(Map query, delegate, boolean isJobs=true, Long seId=null){
        def eqfilters = [
                stat: 'status',
                reportId: 'reportId',
        ]
        def jobfilters = [
                jobId: 'jcJobId',
                proj: 'ctxProject',
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
                        exists(new DetachedCriteria(ReferencedExecution, "re").build {
                            projections { property 're.execution.id' }
                            eq('re.scheduledExecution.id', seId)
                            eqProperty('re.execution.id', 'this.executionId')
                            List execProjectsPartitioned = Lists.partition(query.execProjects, 1000)
                            or{
                                for(def partition : execProjectsPartitioned){
                                    'in'('this.ctxProject', partition)
                                }
                            }
                        })
                        eq('jcJobId', String.valueOf(seId))
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
                        eq('jcJobId', '')
                        isNull('jcJobId')
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
                    isNotNull("jcJobId")
                    isNotNull("executionId")
                }
            } else {
                isNull("jcJobId")
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

}

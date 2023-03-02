package org.rundeck.app.data.providers

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.rundeck.app.components.jobs.JobQuery
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.app.data.model.v1.page.Page
import org.rundeck.app.data.model.v1.page.Pageable
import org.rundeck.app.data.model.v1.query.JobQueryInputData
import org.rundeck.app.data.providers.v1.job.JobQueryProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import rundeck.ScheduledExecution
import rundeck.data.job.RdJobDataSummary
import rundeck.data.job.query.JobQueryConstants
import rundeck.data.paging.RdPageable
import rundeck.services.JobSchedulesService

@CompileStatic
class GormJobQueryProvider implements JobQueryProvider {
    @Autowired
    ApplicationContext applicationContext
    @Autowired
    JobSchedulesService jobSchedulesService

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Page<JobData> getAllScheduledJobs(String project, String serverUUID, Pageable pageable) {
        def results = ScheduledExecution.scheduledJobs()
        if (serverUUID) {
            results = results.withServerUUID(serverUUID)
        }
        if(project) {
            results = results.withProject(project)
        }
        def recs = results.list()
        new GormPage<JobData>(results: recs, total: recs.size())
    }

    private static final int UUID       = 0
    private static final int JOB_NAME   = 1
    private static final int GRP_PATH   = 2
    private static final int DESC       = 3
    private static final int SCHED_EN   = 4
    private static final int EXEC_EN    = 5
    private static final int SCHEDULED  = 6

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Page<JobDataSummary> listJobsByProject(String project, Pageable pageable) {
        GormPage<JobDataSummary> page = new GormPage<>();
        page.results = ScheduledExecution.createCriteria().list {
            maxResults(pageable.max)
            firstResult(pageable.offset)
            order("jobName", "asc")
            eq("project", project)
            projections {
                property "uuid"
                property "jobName"
                property "groupPath"
                property "description"
                property "scheduleEnabled"
                property "executionEnabled"
                property "scheduled"
            }
        }.collect { job ->
            return new RdJobDataSummary(
                    uuid: job[UUID],
                    jobName: job[JOB_NAME],
                    groupPath: job[GRP_PATH],
                    project: project,
                    description: job[DESC],
                    scheduled: job[SCHEDULED],
                    scheduleEnabled: job[SCHED_EN],
                    executionEnabled: job[EXEC_EN]
            )
        }
        page.setPageable(pageable)
        page.setTotal(ScheduledExecution.countByProject(project))
        return page
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    Page<JobDataSummary> queryJobs(JobQueryInputData jobQueryInput) {
        JobQueryInputData query = jobQueryInput

        Integer queryMax=query.max
        Integer queryOffset=query.offset

        if(query.paginatedRequired) {
            if (!queryOffset) {
                queryOffset = 0
            }
        }

        def idlist = createIdList(query)
        def crit = createCriteria()

        def scheduled = crit.list{
            if(queryMax && queryMax>0){
                maxResults(queryMax)
            }
            if(queryOffset){
                firstResult(queryOffset.toInteger())
            }

            applyIdCriteria(idlist, delegate)
            applyTxtFiltersCriteria(query, delegate)
            applyEqFiltersCriteria(query, delegate)
            applyBoolFiltersCriteria(query,delegate)
            applyGroupPathCriteria(query, delegate)
            applyJobComponentCriteria(query, delegate)
            applySort(query, delegate)
        }
        def schedlist = scheduled.collect { se ->
            def summary = se.toJobDataSummary()
            summary.scheduled = jobSchedulesService.isScheduled(se.uuid)
            summary
        }
        def total = schedlist.size()
        if(queryMax && queryMax>0) {
            //count full result set
            def criteria = createCriteria()
            total = criteria.count {

                applyIdCriteria(idlist, delegate)
                applyTxtFiltersCriteria(query, delegate)
                applyEqFiltersCriteria(query, delegate)
                applyBoolFiltersCriteria(query, delegate)
                applyGroupPathCriteria(query, delegate)
                applyJobComponentCriteria(query, delegate)

            }
        }
        def page = new GormPage<JobData>()
        page.results = schedlist
        page.total = total
        page.pageable = new RdPageable(max: query.max, offset: query.offset, sortOrders: query.sortOrders)
        return page
    }

    BuildableCriteria createCriteria() {
        return ScheduledExecution.createCriteria()
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyJobComponentCriteria(JobQueryInputData query, BuildableCriteria crit) {
        applicationContext.getBeansOfType(JobQuery).each { name, jobQuery ->
            jobQuery.extendCriteria(query, query.inputParamMap, crit)
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applySort(JobQueryInputData query, BuildableCriteria crit) {
        def xfilters = ScheduledExecutionQuery.X_FILTERS
        if(query && query.sortBy && xfilters[query.sortBy]){
            crit.order(xfilters[query.sortBy],query.sortOrder=='asc'?'asc':'desc')
        }else{
            if(query.paginatedRequired) {
                crit.order("groupPath","asc")
            }
            crit.order("jobName","asc")
        }
    }

    void applyIdCriteria(List idlist, BuildableCriteria crit) {
        if (idlist) {
            crit.or {
                idlist.each { theid ->
                    if (theid instanceof Long) {
                        crit.eq("id", theid)
                    } else {
                        crit.eq("uuid", theid)
                    }
                }
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyTxtFiltersCriteria(JobQueryInputData query, BuildableCriteria crit) {
        JobQueryConstants.TEXT_FILTERS.each{ key, val ->
            if(query["${key}Filter"]){
                crit.ilike(val,'%'+query["${key}Filter"]+'%')
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyEqFiltersCriteria(JobQueryInputData query, BuildableCriteria crit) {
        JobQueryConstants.EQ_FILTERS.each { key, val ->
            if (query["${key}Filter"]) {
                crit.eq(val, query["${key}Filter"])
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyBoolFiltersCriteria(JobQueryInputData query, BuildableCriteria crit) {
        JobQueryConstants.BOOL_FILTERS.each{ key,val ->
            if(null!=query["${key}Filter"]){
                crit.eq(val, query["${key}Filter"])
            }
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void applyGroupPathCriteria(JobQueryInputData query, BuildableCriteria crit) {
        if('-'==query["groupPath"]||"-"==query["groupPathExact"]){
            crit.or {
                eq("groupPath", "")
                isNull("groupPath")
            }
        }else if(query["groupPath"] &&  '*'!=query["groupPath"]){
            crit.or {
                crit.like("groupPath",query["groupPath"]+"/%")
                eq("groupPath",query['groupPath'])
            }
        }else if(query["groupPathExact"]){
            crit.or{
                eq("groupPath",query['groupPathExact'])
            }
        }
    }

    def createIdList(JobQueryInputData query) {
        def idlist=[]
        if(query.idlist){

            def arr = query.idlist.split(",")
            arr.each{
                try{
                    idlist<<Long.valueOf(it)
                }catch(NumberFormatException e){
                    idlist<<it
                }
            }

        }
        return idlist
    }

    static class GormPage<T> implements Page<T> {
        List<T> results = []
        Long total
        Pageable pageable
    }
}

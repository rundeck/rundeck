package rundeck.services

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRoles
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.plugins.quartz.listeners.SessionBinderJobListener
import grails.transaction.Transactional
import org.apache.log4j.Logger
import org.apache.log4j.MDC
import org.hibernate.StaleObjectStateException
import org.quartz.*
import org.quartz.impl.matchers.KeyMatcher
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.transaction.annotation.Propagation
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.*
import rundeck.controllers.EditOptsController
import rundeck.controllers.JobXMLException
import rundeck.controllers.ScheduledExecutionController
import rundeck.controllers.WorkflowController
import rundeck.quartzjobs.ExecutionJob

import javax.servlet.http.HttpSession
import java.text.MessageFormat
import java.text.SimpleDateFormat

/**
 *  ScheduledExecutionService manages scheduling jobs with the Quartz scheduler
 */
class ScheduledExecutionService implements ApplicationContextAware, InitializingBean{
    boolean transactional = true

    def FrameworkService frameworkService
    def NotificationService notificationService
    //private field to set lazy bean dependency
    private ExecutionService executionServiceBean
    def executorService
    def Scheduler quartzScheduler
    /**
     * defined in quartz plugin
     */
    def SessionBinderJobListener sessionBinderListener
    ApplicationContext applicationContext

    def MessageSource messageSource
    def grailsEvents

    @Override
    void afterPropertiesSet() throws Exception {
        //add listener for every job
        quartzScheduler?.getListenerManager()?.addJobListener(sessionBinderListener)
    }

    /**
     * private getter for executionService that is not auto-injected
     * @return
     */
    private ExecutionService getExecutionService(){
        if(null==executionServiceBean){
            this.executionServiceBean = applicationContext.getBean("executionService",ExecutionService)
        }
        return executionServiceBean
    }


    def Map finishquery ( query,params,model){

        if(!params.max){
            params.max=20
        }
        if(!params.offset){
            params.offset=0
        }

        def paginateParams=[:]
        if(query){
            model._filters.each{ key,val ->
                if(null!=query."${key}Filter" && !''.equals(query."${key}Filter")){
                    paginateParams["${key}Filter"]=query."${key}Filter"
                }
            }
            if(null!=query.'idlist' && !''.equals(query.'idlist')){
                paginateParams['idlist']=query.'idlist'
            }
        }
        def displayParams = [:]
        displayParams.putAll(paginateParams)

        if(query.groupPath && query.groupPath!="*"){
            paginateParams['groupPath']=query.groupPath
        }else{
            params.groupPath=null
        }


        def tmod=[max: query?.max?query.max:20,
            offset:query?.offset?query.offset:0,
            paginateParams:paginateParams,
            displayParams:displayParams]
        model.putAll(tmod)
        return model
    }
    def listWorkflows(ScheduledExecutionQuery query) {
        def txtfilters = ScheduledExecutionQuery.TEXT_FILTERS
        def eqfilters=ScheduledExecutionQuery.EQ_FILTERS
        def boolfilters=ScheduledExecutionQuery.BOOL_FILTERS
        def filters = ScheduledExecutionQuery.ALL_FILTERS

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
        if(!query.groupPath && !query.groupPathExact){
            query.groupPath='*'
        }else if('-'==query.groupPath){
            query.groupPath=null
        }

        def crit = ScheduledExecution.createCriteria()

        def scheduled = crit.list{
            if(query?.max && query.max.toInteger()>0){
                maxResults(query.max.toInteger())
            }else{
//                maxResults(10)
            }
            if(query.offset){
                firstResult(query.offset.toInteger())
            }

            if(idlist){
                or{
                    idlist.each{ theid->
                        if(theid instanceof Long){
                            eq("id",theid)
                        }else{
                            eq("uuid", theid)
                        }
                    }
                }
            }

            txtfilters.each{ key,val ->
                if(query["${key}Filter"]){
                    ilike(val,'%'+query["${key}Filter"]+'%')
                }
            }

            eqfilters.each{ key,val ->
                if(query["${key}Filter"]){
                    eq(val,query["${key}Filter"])
                }
            }
            boolfilters.each{ key,val ->
                if(null!=query["${key}Filter"]){
                    eq(val,query["${key}Filter"])
                }
            }


            if('*'==query["groupPath"]){
                //don't filter out any grouppath
            }else if(query["groupPath"]){
                or{
                    like("groupPath",query["groupPath"]+"/%")
                    eq("groupPath",query['groupPath'])
                }
            }else if(query["groupPathExact"]){
                if("-"==query["groupPathExact"]){

                    or {
                        eq("groupPath", "")
                        isNull("groupPath")
                    }
                }else{
                    or{
                        eq("groupPath",query['groupPathExact'])
                    }
                }
            }else{
                or{
                    eq("groupPath","")
                    isNull("groupPath")
                }
            }

            if(query && query.sortBy && filters[query.sortBy]){
                order(filters[query.sortBy],query.sortOrder=='ascending'?'asc':'desc')
            }else{
                order("jobName","asc")
            }
        };
        def schedlist = [];
        scheduled.each{
            schedlist << it
        }

        def total = schedlist.size()
        if(query?.max && query.max.toInteger()>0) {
            //count full result set
            total = ScheduledExecution.createCriteria().count {

                if (idlist) {
                    or {
                        idlist.each { theid ->
                            if (theid instanceof Long) {
                                eq("id", theid)
                            } else {
                                eq("uuid", theid)
                            }
                        }
                    }
                }

                txtfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        ilike(val, '%' + query["${key}Filter"] + '%')
                    }
                }
                eqfilters.each { key, val ->
                    if (query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }
                boolfilters.each { key, val ->
                    if (null != query["${key}Filter"]) {
                        eq(val, query["${key}Filter"])
                    }
                }


                if ('*' == query["groupPath"]) {
                    //don't filter out any grouppath
                } else if (query["groupPath"]) {
                    or {
                        like("groupPath", query["groupPath"] + "/%")
                        eq("groupPath", query['groupPath'])
                    }
                } else {
                    or {
                        eq("groupPath", "")
                        isNull("groupPath")
                    }
                }
            }
        }


        return [
            query:query,
            schedlist:schedlist,
            total: total,
            _filters:filters
            ]

    }


    /**
     * return a map of defined group path to count of the number of jobs with that exact path
     */
    def Map getGroups(project, AuthContext authContext){
        def groupMap=[:]

        //collect all jobs and authorize the user for the set of available Job actions
        Set res = new HashSet()
        def schedlist= ScheduledExecution.findAllByProject(project)
        schedlist.each { ScheduledExecution sched ->
            res.add(frameworkService.authResourceForJob(sched))
        }
        // Filter the groups by what the user is authorized to see.

        def decisions = frameworkService.authorizeProjectResources(authContext,res,
            new HashSet([AuthConstants.ACTION_READ]),project)

        decisions.each{
            if(it.authorized){
                if(null!= groupMap[it.resource['group']]){
                    groupMap[it.resource['group']]= groupMap[it.resource['group']]+1
                }else{
                    groupMap[it.resource['group']]=1
                }
            }
        }
        return groupMap
    }

    /**
     * Claim schedule for a job with the passed in serverUUID
     * @param scheduledExecution
     * @param serverUUID uuid to assign to the scheduled job
     * @return
     */
    private boolean claimScheduledJob(ScheduledExecution scheduledExecution, String serverUUID, String fromServerUUID=null){
        def schedId=scheduledExecution.id
        def claimed=false
        while (!claimed) {
            try {
                ScheduledExecution.withNewSession {
                    scheduledExecution = ScheduledExecution.get(schedId)
                    scheduledExecution.refresh()

                    scheduledExecution.serverNodeUUID=serverUUID
                    if (scheduledExecution.save(flush: true)) {
                        claimed=true
                        log.info("claimScheduledJob: schedule claimed for ${schedId} on node ${serverUUID}")
                    } else {
                        log.debug("claimScheduledJob: failed for ${schedId} on node ${serverUUID}")
                    }
                }
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                log.error("claimScheduledJob: failed for ${schedId} on node ${serverUUID}: locking failure")
            } catch (StaleObjectStateException e) {
                log.error("claimScheduledJob: failed for ${schedId} on node ${serverUUID}: stale data")
            }
        }
        return claimed
    }

    /**
     * Claim scheduling for any jobs assigned to fromServerUUID, or not assigned if it is null
     * @param toServerUUID uuid to assign to scheduled jobs
     * @param fromServerUUID uuid to claim from, or null to claim from unassigned jobs
     *
     * @return Map of job ID to boolean, indicating whether the job was claimed
     */
    def Map claimScheduledJobs(
            String toServerUUID,
            String fromServerUUID = null,
            boolean selectAll = false,
            String projectFilter = null,
            String jobid = null
    )
    {
        Map claimed = [:]
        def queryFromServerUUID = fromServerUUID
        def queryProject = projectFilter
        ScheduledExecution.withTransaction {
            ScheduledExecution.withCriteria {
                eq('scheduled', true)
                if (!selectAll) {
                    if (queryFromServerUUID) {
                        eq('serverNodeUUID', queryFromServerUUID)
                    } else {
                        isNull('serverNodeUUID')
                    }
                } else {
                    ne('serverNodeUUID', toServerUUID)
                }
                if (queryProject) {
                    eq('project', queryProject)
                }
                if(jobid){
                    eq('uuid',jobid)
                }
            }.each { ScheduledExecution se ->
                def orig = se.serverNodeUUID
                claimed[se.extid] = [success: claimScheduledJob(
                        se,
                        toServerUUID,
                        queryFromServerUUID
                ), job                      : se, previous: orig]
            }
        }
        claimed
    }
    /**
     * Remove all scheduling for job executions, triggered when passive mode is enabled
     * @param serverUUID
     */
    def unscheduleJobs(String serverUUID=null){
        def schedJobs = serverUUID ? ScheduledExecution.findAllByScheduledAndServerNodeUUID(true, serverUUID) : ScheduledExecution.findAllByScheduled(true)
        schedJobs.each { ScheduledExecution se ->
            def jobname = se.generateJobScheduledName()
            def groupname = se.generateJobGroupName()

            quartzScheduler.deleteJob(new JobKey(jobname,groupname))
            log.info("Unscheduled job: ${se.id}")
        }
    }

    def rescheduleJob(ScheduledExecution scheduledExecution) {
        rescheduleJob(scheduledExecution, false, null, null)
    }

    def rescheduleJob(ScheduledExecution scheduledExecution, wasScheduled, oldJobName, oldJobGroup) {
        if (scheduledExecution.shouldScheduleExecution()) {
            def nextdate = null
            try {
                nextdate = scheduleJob(scheduledExecution, oldJobName, oldJobGroup);
            } catch (SchedulerException e) {
                log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
            }
            def newsched = ScheduledExecution.get(scheduledExecution.id)
            newsched.nextExecution = nextdate
            if (!newsched.save()) {
                log.error("Unable to save second change to scheduledExec.")
            }
        } else if (wasScheduled && oldJobName && oldJobGroup) {
            deleteJob(oldJobName, oldJobGroup)
        }
    }

    /**
     * Reschedule all scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @return
     */
    def rescheduleJobsAsync(String serverUUID=null) {
        executorService.execute{
            rescheduleJobs(serverUUID)
        }
    }
    /**
     * Reschedule all scheduled jobs which match the given serverUUID, or all jobs if it is null.
     * @param serverUUID
     * @return
     */
    def rescheduleJobs(String serverUUID=null) {
        def schedJobs = serverUUID ? ScheduledExecution.findAllByScheduledAndServerNodeUUID(true, serverUUID) : ScheduledExecution.findAllByScheduled(true)
        schedJobs.each { ScheduledExecution se ->
            try {
                scheduleJob(se, null, null)
                log.info("rescheduled job: ${se.id}")
            } catch (Exception e) {
                log.debug("Job not rescheduled: ${se.id}: ${e.message}",e)
                log.error("Job not rescheduled: ${se.id}: ${e.message}")
            }
        }
    }
    /**
     * Claim scheduling of jobs from the given fromServerUUID, and return a map identifying successfully claimed jobs
     * @param fromServerUUID server UUID to claim scheduling of jobs from
     * @return map of job ID to [success:boolean, job:ScheduledExecution] indicating reclaim was successful or not.
     */
    def reclaimAndScheduleJobs(String fromServerUUID, boolean all=false, String project=null, String id=null){
        def claimed=claimScheduledJobs(frameworkService.getServerUUID(), fromServerUUID, all, project, id)
        rescheduleJobs(frameworkService.getServerUUID())
        claimed
    }

    /**
     *  Return a Map with a tree structure of the available grouppaths, and their job counts
     * <pre>
          [
            'a' :[
                count:6,
                subs:[
                    'b': [
                        count:4,
                    ],
                    'c': [
                        count:1,
                        subs:[
                            'd': [
                            count: 1,

                            ]
                        ]
                    ]
                ]
             ]

          ]
     * </pre>
     */
    def Map getGroupTree(project, AuthContext authContext){
        def groupMap = getGroups(project, authContext)
        def tree=[:]
        groupMap.keySet().each{
            tree[it]=[]
        }
        return tree
    }

    def listNextScheduledJobs(int num){
        def list = ScheduledExecution.list(max: num, sort:'nextExecution')
        return list;
    }
    /**
     * Delete all executions for a job. Return a map with results, as {@link ExecutionService#deleteBulkExecutionIds(java.util.Collection, com.dtolabs.rundeck.core.authorization.AuthContext, java.lang.String)}
     * @param scheduledExecution
     * @param authContext @param var
     */
    def deleteJobExecutions(ScheduledExecution scheduledExecution, AuthContext authContext, def username){
        Execution.withTransaction {
            //unlink any Execution records
            def executions = Execution.findAllByScheduledExecution(scheduledExecution)
            def results=executionService.deleteBulkExecutionIds(executions*.id, authContext, username)
            return results
        }
    }

    /**
     * Immediately delete a ScheduledExecution
     * @param username @param scheduledExecution
     * @return
     */
    def deleteScheduledExecution(ScheduledExecution scheduledExecution, boolean deleteExecutions=false,
                                 AuthContext authContext=null, String username){
        scheduledExecution = ScheduledExecution.get(scheduledExecution.id)
        def originalRef=jobEventRevRef(scheduledExecution)
        def jobname = scheduledExecution.generateJobScheduledName()
        def groupname = scheduledExecution.generateJobGroupName()
        def errmsg=null
        def success = false
        Execution.withTransaction {
            //find any currently running executions for this job, and if so, throw exception
            def found = Execution.createCriteria().get {
                delegate.'scheduledExecution' {
                    eq('id', scheduledExecution.id)
                }
                isNotNull('dateStarted')
                isNull('dateCompleted')
            }

            if (found) {
                errmsg = 'Cannot delete {{Job ' + scheduledExecution.extid + '}} "' + scheduledExecution.jobName  +
                        '" it is currently being executed: {{Execution ' + found.id + '}}'
                return [success:false,error:errmsg]
            }
            //unlink any Execution records
            def result = Execution.findAllByScheduledExecution(scheduledExecution)
            if(deleteExecutions){
                executionService.deleteBulkExecutionIds(result*.id,authContext, username)
            }else{

                result.each { Execution exec ->
                    exec.scheduledExecution = null
                }
            }
            try {
                scheduledExecution.delete(flush: true)
                deleteJob(jobname, groupname)
                success = true
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                scheduledExecution.discard()
                errmsg = 'Cannot delete Job "' + scheduledExecution.jobName + '" [' + scheduledExecution.extid + ']: it may have been modified or executed by another user'
            } catch (StaleObjectStateException e) {
                scheduledExecution.discard()
                errmsg = 'Cannot delete Job "' + scheduledExecution.jobName + '" [' + scheduledExecution.extid + ']: it may have been modified or executed by another user'
            }
        }
        if(success){

            def event = createJobChangeEvent(JobChangeEvent.JobChangeEventType.DELETE, originalRef)

            //issue event directly
            grailsEvents?.event(null, 'jobChanged', event)
        }
        return [success:success,error:errmsg]
    }
    /**
     * Attempt to delete a job given an id
     * @param jobid
     * @param original auth context
     * @param deleteBulkExecutions true to delete all executions of the job
     * @param user user requesting delete action
     * @param callingAction name of action/method requesting delete for logging
     *
     * @return map [error: [message: String, errorCode: String, id: String, job: ScheduledExecution?], success:boolean]
     */
    def deleteScheduledExecutionById(jobid, AuthContext original, boolean deleteExecutions, String user,
    String callingAction){

        def ScheduledExecution scheduledExecution = getByIDorUUID(jobid)
        if (!scheduledExecution) {
            def err = [
                    message: lookupMessage( "api.error.item.doesnotexist",  ['Job ID', jobid] as Object[]),
                    errorCode: 'notfound',
                    id: jobid
            ]
            return [error: err,success: false]
        }

        //extend auth context using project-specific authorization
        AuthContext authContext = frameworkService.getAuthContextWithProject(original, scheduledExecution.project)

        if (!frameworkService.authorizeProjectResource(
                authContext,
                AuthConstants.RESOURCE_TYPE_JOB,
                AuthConstants.ACTION_DELETE,
                scheduledExecution.project
        ) || !frameworkService.authorizeProjectJobAll(
                authContext,
                scheduledExecution,
                [AuthConstants.ACTION_DELETE],
                scheduledExecution.project
        )) {
            def err = [
                    message: lookupMessage('api.error.item.unauthorized', ['Delete', 'Job ID', scheduledExecution.extid] as Object[]),
                    errorCode: 'unauthorized',
                    id: scheduledExecution.extid,
                    job: scheduledExecution
            ]
            return [error: err,success: false]
        }
        def changeinfo = [user: user, method: callingAction, change: 'delete']
        def jobdata = scheduledExecution.properties
        def jobtitle = "[" + scheduledExecution.extid + "] " + scheduledExecution.generateFullName()
        def result = deleteScheduledExecution(scheduledExecution, deleteExecutions, authContext, user)
        if (!result.success) {
            return [success:false,error:  [message: result.error, job: scheduledExecution, errorCode: 'failed', id: scheduledExecution.extid]]
        } else {
            logJobChange(changeinfo, jobdata)
            return [success: [message: lookupMessage('api.success.job.delete.message', [jobtitle] as Object[]), job: scheduledExecution]]
        }
    }
    /**
     * Delete a quartz job by name/group
     * @param jobname
     * @param groupname
     * @return
     */
    def deleteJob(String jobname, String groupname){
        log.info("deleting job from scheduler")
        quartzScheduler.deleteJob(new JobKey(jobname,groupname))
    }

    def userAuthorizedForJob(request,ScheduledExecution se, AuthContext authContext){
        return frameworkService.authorizeProjectJobAll(authContext,se,[AuthConstants.ACTION_READ],se.project)
    }
    def userAuthorizedForAdhoc(request,ScheduledExecution se, AuthContext authContext){
        return frameworkService.authorizeProjectResource(authContext, AuthConstants.RESOURCE_ADHOC,
                AuthConstants.ACTION_RUN,se.project)
    }

    def scheduleJob(ScheduledExecution se, String oldJobName, String oldGroupName) {
        if(!executionService.executionsAreActive){
            log.warn("Attempt to schedule job ${se}, but executions are disabled.")
            return null
        }

        if (!se.shouldScheduleExecution()) {
            log.warn("Attempt to schedule job ${se}, but job execution is disabled.")
            return null;
        }

        def jobDetail = createJobDetail(se)
        def trigger = createTrigger(se)
        jobDetail.getJobDataMap().put("bySchedule", true)
        def Date nextTime
        if(oldJobName && oldGroupName){
            log.info("job renamed, removing old job and scheduling new one")
            deleteJob(oldJobName,oldGroupName)
        }
        if ( hasJobScheduled(se) ) {
            log.info("rescheduling existing job: " + se.generateJobScheduledName())
            
            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(se.generateJobScheduledName(), se.generateJobGroupName()), trigger)
        } else {
            log.info("scheduling new job: " + se.generateJobScheduledName())
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        log.info("scheduled job. next run: " + nextTime.toString())
        return nextTime
    }
    def boolean existsJob(String jobname, String groupname){

        def exists=false
        quartzScheduler.getCurrentlyExecutingJobs().each{ def JobExecutionContext jexec ->

            if(jexec.getJobDetail().getName()==jobname && jexec.getJobDetail().getGroup()==groupname){
                def job = jexec.getJobInstance()
                if(job ){
                    exists=true
                }
            }
        }
        return exists
    }
    def boolean interruptJob(String jobname, String groupname){

        def didcancel=false
        quartzScheduler.getCurrentlyExecutingJobs().each{ def JobExecutionContext jexec ->

            if(jexec.getJobDetail().getName()==jobname && jexec.getJobDetail().getGroup()==groupname){
                def job = jexec.getJobInstance()
                if(job && job instanceof InterruptableJob){
                    job.interrupt()
                    didcancel=true
                }
            }
        }
        return didcancel
    }

    def Map getJobIdent(ScheduledExecution se, Execution e){
        if(!se){
            return [jobname:"TEMP:"+e.user +":"+e.id, groupname:e.user+":run"]
        }
        else if(se.scheduled){
            return [jobname:se.generateJobScheduledName(),groupname:se.generateJobGroupName()]
        }else{
            return [jobname:"TEMP:"+e.user +":"+se.id+":"+e.id, groupname:e.user+":run:"+se.id]
        }
    }

    /**
     * Schedule a stored job to execute immediately, include a set of params in the data map
     */
    def long scheduleTempJob(ScheduledExecution se, String user, AuthContext authContext,
                             Execution e, Map secureOpts =null,
                             Map secureOptsExposed =null, int retryAttempt = 0) {

        def quartzjobname="TEMP:" + user + ":" + se.id + ":" + e.id
        def jobDetail = createJobDetail(se, quartzjobname,user + ":run:" + se.id)
        jobDetail.getJobDataMap().put("user", user)
        jobDetail.getJobDataMap().put("authContext", authContext)
        jobDetail.getJobDataMap().put("executionId", e.id.toString())
        if(secureOpts){
            jobDetail.getJobDataMap().put("secureOpts", secureOpts)
        }
        if(secureOptsExposed){
            jobDetail.getJobDataMap().put("secureOptsExposed", secureOptsExposed)
        }
        if(retryAttempt){
            jobDetail.getJobDataMap().put("retryAttempt",retryAttempt)
        }else{
            jobDetail.getJobDataMap().put("retryAttempt", 0)
        }

        def Trigger trigger = TriggerBuilder.newTrigger().startNow().withIdentity(quartzjobname + "Trigger").build()

        def nextTime
        try {
            log.info("scheduling immediate job run: " + quartzjobname)
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        } catch (Exception exc) {
            throw new RuntimeException("caught exception while adding job: " + exc.getMessage(), exc)
        }
        return e.id
    }

    /**
     * Schedule a temp job to execute immediately.
     */
    def Map scheduleTempJob(AuthContext authContext, Execution e) {
        if(!executionService.getExecutionsAreActive()){
            def msg=g.message(code:'disabled.execution.run')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        if (!e.hasExecutionEnabled()) {
            def msg=g.message(code:'scheduleExecution.execution.disabled')
            return [success:false,failed:true,error:'disabled',message:msg]
        }

        def ident = getJobIdent(null, e);
        def jobDetail = JobBuilder.newJob(ExecutionJob)
                .withIdentity(ident.jobname, ident.groupname)
                .withDescription("Execute command: " + e)
                .usingJobData(
                    new JobDataMap(
                        [
                            'isTempExecution': 'true',
                            'executionId': e.id.toString(),
                            'authContext': authContext
                        ]
                    )
                )
                .build()



        def Trigger trigger = TriggerBuilder.newTrigger().withIdentity(ident.jobname + "Trigger").startNow().build()
        def nextTime
        try {
            log.info("scheduling temp job: " + ident.jobname)
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        } catch (Exception exc) {
            throw new RuntimeException("caught exception while adding job: " + exc.getMessage(), exc)
        }
        return [success:true,execution:e,id:e.id]
    }

    def JobDetail createJobDetail(ScheduledExecution se) {
        return createJobDetail(se,se.generateJobScheduledName(), se.generateJobGroupName())
    }


    def JobDetail createJobDetail(ScheduledExecution se, String jobname, String jobgroup){
        def jobDetailBuilder = JobBuilder.newJob(ExecutionJob).withIdentity(jobname,jobgroup)
                        .withDescription(se.description)
                .usingJobData("scheduledExecutionId",se.id.toString())
                .usingJobData("rdeck.base",frameworkService.getRundeckBase())

        if(se.scheduled){
            jobDetailBuilder.usingJobData("userRoles",se.userRoleList)
            if(frameworkService.isClusterModeEnabled()){
                jobDetailBuilder.usingJobData("serverUUID",frameworkService.getServerUUID())
            }
        }

        return jobDetailBuilder.build()
    }

    def Trigger createTrigger(ScheduledExecution se) {
        def Trigger trigger
        def cronExpression = se.generateCrontabExression()
        try {
            log.info("creating trigger with crontab expression: " + cronExpression)
            trigger = TriggerBuilder.newTrigger().withIdentity(se.generateJobScheduledName(), se.generateJobGroupName())
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build()
        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    def boolean hasJobScheduled(ScheduledExecution se) {
        return quartzScheduler.checkExists(JobKey.jobKey(se.generateJobScheduledName(),se.generateJobGroupName()))
    }

    /**
     * Return a map of job ID to next trigger Date
     * @param scheduledExecutions
     * @return
     */
    def Map nextExecutionTimes(Collection<ScheduledExecution> scheduledExecutions) {
        def map = [ : ]
        scheduledExecutions.each {
            def next = nextExecutionTime(it)
            if(next){
                map[it.id] = next
            }
        }
        return map
    }

    /**
     * Return a map of job ID to serverNodeUUID for any jobs which are scheduled on a different server, if cluster mode is enabled.
     * @param scheduledExecutions
     * @return
     */
    def Map clusterScheduledJobs(Collection<ScheduledExecution> scheduledExecutions) {
        def map = [ : ]
        if(frameworkService.isClusterModeEnabled()) {
            def serverUUID = frameworkService.getServerUUID()
            scheduledExecutions.findAll { it.serverNodeUUID != serverUUID }.each {
                map[it.id] = it.serverNodeUUID
            }
        }
        return map
    }

    public static final long TWO_HUNDRED_YEARS=1000l * 60l * 60l * 24l * 365l * 200l
    /**
     * Return the next scheduled or predicted execution time for the scheduled job, and if it is not scheduled
     * return a time in the future.  If the job is not scheduled on the current server (cluster mode), returns
     * the time that the job is expected to run on its configured server.
     * @param se
     * @return
     */
    def Date nextExecutionTime(ScheduledExecution se) {
        if(!se.scheduled){
            return new Date(TWO_HUNDRED_YEARS)
        }
        def trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(se.generateJobScheduledName(), se.generateJobGroupName()))
        if(trigger){
            return trigger.getNextFireTime()
        }else if (frameworkService.isClusterModeEnabled() && se.serverNodeUUID != frameworkService.getServerUUID()) {
            //guess next trigger time for the job on the assigned cluster node
            def value= tempNextExecutionTime(se)
            return value
        } else {
            return null;
        }
    }

    /**
     * Return the Date for the next execution time for a scheduled job
     * @param se
     * @return
     */
    def Date tempNextExecutionTime(ScheduledExecution se){
        def trigger = createTrigger(se)
        return trigger.getFireTimeAfter(new Date())
    }

    /**
     * Find a ScheduledExecution by UUID or ID.  Checks if the
     * input value is a Long, if so finds the ScheduledExecution with that ID.
     * If it is a String it attempts to parse the String as a Long and if it is
     * valid it finds the ScheduledExecution by ID. Otherwise it attempts to find the ScheduledExecution with that
     * UUID.
     * @param anid
     * @return ScheduledExecution found or null
     */
    def ScheduledExecution getByIDorUUID(anid){
        ScheduledExecution.getByIdOrUUID(anid)
    }

    /**
     * Get the locale
     * @return locale
     * */
    def getLocale() {
        def Locale locale = null
        try {
            locale = RequestContextUtils.getLocale(getSession().request)
        }
        catch (java.lang.IllegalStateException e) {
            //log.debug "Running in console?"
        }
        //log.debug "locale: ${locale}"
        return locale
    }
    /**
     * @parameter key
     * @returns corresponding value from messages.properties
     */
    def lookupMessage(String theKey, Object[] data, String defaultMessage = null) {
        def locale = getLocale()
        def theValue = null
//        MessageSource messageSource = applicationContext.getBean("messageSource")
        try {
            theValue = messageSource.getMessage(theKey, data, locale)
        } catch (org.springframework.context.NoSuchMessageException e) {
            log.error "Missing message ${theKey}"
//        } catch (java.lang.NullPointerException e) {
//            log.error "Expression does not exist: ${theKey}: ${e}"
        }
        if (null == theValue && defaultMessage) {
            MessageFormat format = new MessageFormat(defaultMessage);
            theValue = format.format(data)
        }
        return theValue
    }
    /**
     * @parameter key
     * @returns corresponding value from messages.properties
     */
    def lookupMessageError(error, String defaultMessage = null) {
        def locale = getLocale()
        def theValue = null
//        MessageSource messageSource = applicationContext.getBean("messageSource")
        try {

            theValue = messageSource.getMessage(error, locale)
        } catch (org.springframework.context.NoSuchMessageException e) {
            log.error "Missing message ${error}"
//        } catch (java.lang.NullPointerException e) {
//            log.error "Expression does not exist: ${error}: ${e}"
        }
        if (null == theValue && defaultMessage) {
            MessageFormat format = new MessageFormat(defaultMessage);
            theValue = format.format(null)
        }
        return theValue
    }
    /**
     * Get the HTTP Session
     * @return session
     * */
    private HttpSession getSession() {
        return RequestContextHolder.currentRequestAttributes().getSession()
    }


    /**
     * Given list of imported jobs, create, update or skip them as defined by the dupeOption parameter.
     * @return map of load results, [jobs: List of ScheduledExecutions, jobsi: list of maps [scheduledExecution: (job), entrynum: (index)], errjobs: List of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg], skipjobs: list of maps [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]]
     */
    def loadJobs (
            List<ScheduledExecution> jobset,
            String option,
            String uuidOption,
            Map changeinfo = [:],
            UserAndRolesAuthContext authContext
    ){
        def jobs = []
        def jobsi = []
        def i = 1
        def errjobs = []
        def skipjobs = []
        def jobChangeEvents=[]
        jobset.each { jobdata ->
            log.debug("saving job data: ${jobdata}")
            def ScheduledExecution scheduledExecution
            def jobchange = new HashMap(changeinfo)
            if(!jobdata.project){
                errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "Project was not specified"]
                i++
                return
            }
            if(!frameworkService.existsFrameworkProject(jobdata.project)){
                errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "Project does not exist: ${jobdata.project}"]
                i++
                return
            }
            if (uuidOption == 'remove') {
                jobdata.uuid = null
                jobdata.id = null
            }
            if (option == "update" || option == "skip") {
                //look for dupe by name and group path and project
                def schedlist
                //first look for uuid
                if (jobdata.uuid && jobdata.project) {
                    scheduledExecution = ScheduledExecution.findByUuidAndProject(jobdata.uuid,jobdata.project)
                } else if(jobdata.jobName && jobdata.project){
                    schedlist = ScheduledExecution.findAllScheduledExecutions(jobdata.groupPath, jobdata.jobName, jobdata.project)
                    if (schedlist && 1 == schedlist.size()) {
                        scheduledExecution = schedlist[0]
                    }
                }
            }

            def project = scheduledExecution ? scheduledExecution.project : jobdata.project

            def projectAuthContext = frameworkService.getAuthContextWithProject(authContext, project)
            if (option == "skip" && scheduledExecution) {
                jobdata.id = scheduledExecution.id
                skipjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: "A Job named '${jobdata.jobName}' already exists"]
            }
            else if (option == "update" && scheduledExecution) {
                def success = false
                def errmsg
                jobchange.change = 'modify'
                if (!frameworkService.authorizeProjectJobAll(projectAuthContext, scheduledExecution, [AuthConstants.ACTION_UPDATE], scheduledExecution.project)) {
                    errmsg = "Unauthorized: Update Job ${scheduledExecution.id}"
                } else {
                    try {
                        def result = _doupdateJob(scheduledExecution.id, jobdata, projectAuthContext, jobchange)
                        success = result.success
                        scheduledExecution = result.scheduledExecution
                        if(success && result.jobChangeEvent){
                            jobChangeEvents<<result.jobChangeEvent
                        }
                        if (!success && scheduledExecution && scheduledExecution.hasErrors()) {
                            errmsg = "Validation errors: "+ scheduledExecution.errors.allErrors.collect{lookupMessageError(it)}.join("; ")
                        } else {
                            logJobChange(jobchange, scheduledExecution.properties)
                        }
                    } catch (Exception e) {
                        errmsg = e.getMessage()
                        System.err.println("caught exception: " + errmsg);
                        e.printStackTrace()
                    }
                }
                if (!success) {
                    errjobs << [scheduledExecution: scheduledExecution, entrynum: i, errmsg: errmsg]
                } else {
                    jobs << scheduledExecution
                    jobsi << [scheduledExecution: scheduledExecution, entrynum: i]
                }
            } else if (option == "create" || !scheduledExecution) {
                def errmsg

                if (!frameworkService.authorizeProjectResourceAll(projectAuthContext, AuthConstants.RESOURCE_TYPE_JOB,
                                                                  [AuthConstants.ACTION_CREATE], jobdata.project)) {
                    errmsg = "Unauthorized: Create Job"
                    errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]
                } else {
                    try {
                        jobchange.change = 'create'
                        def result = _dosave(jobdata, projectAuthContext, jobchange)
                        scheduledExecution = result.scheduledExecution
                        if (!result.success && scheduledExecution && scheduledExecution.hasErrors()) {
                            errmsg = "Validation errors: " + scheduledExecution.errors.allErrors.collect { lookupMessageError(it) }.join("; ")
                        } else if (!result.success) {
                            errmsg = result.error ?: "Failed to save job"
                        } else {
                            logJobChange(jobchange, scheduledExecution.properties)
                            jobChangeEvents<<result.jobChangeEvent
                        }
                    } catch (Exception e) {
                        System.err.println("caught exception");
                        e.printStackTrace()
                        scheduledExecution = jobdata
                        errmsg = e.getMessage()
                    }
                    if (scheduledExecution && !scheduledExecution.id) {
                        errjobs << [scheduledExecution: scheduledExecution, entrynum: i, errmsg: errmsg]
                    } else if (!scheduledExecution) {
                        errjobs << [scheduledExecution: jobdata, entrynum: i, errmsg: errmsg]
                    } else {
                        jobs << scheduledExecution
                        jobsi << [scheduledExecution: scheduledExecution, entrynum: i]
                    }
                }
            }

            i++

        }
        return [jobs: jobs, jobsi: jobsi, errjobs: errjobs, skipjobs: skipjobs,jobChangeEvents:jobChangeEvents]
    }
    static Logger jobChangeLogger = Logger.getLogger("com.dtolabs.rundeck.data.jobs.changes")

    def logJobChange(data, jobdata) {
        data.keySet().each {k ->
            def v = data[k]
            if (v instanceof Date) {
                //TODO: reformat date
                MDC.put(k, v.toString())
                MDC.put("${k}Time", v.time.toString())
            } else if (v instanceof String) {
                MDC.put(k, v ? v : "-")
            } else {
                final string = v.toString()
                MDC.put(k, string ? string : "-")
            }
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            final var = jobdata[k]
            MDC.put(k, var ? var : '-')
        }
        if (jobdata.uuid) {
            MDC.put('id', jobdata.uuid)
        }
        final msg = data.user + " " + data.change.toUpperCase() + " [" + (jobdata.uuid ?: jobdata.id) + "] " + jobdata.project + " \"" + (jobdata.groupPath ? jobdata.groupPath : '') + "/" + jobdata.jobName + "\" (" + data.method + ")"
        jobChangeLogger.info(msg)
        data.keySet().each {k ->
            if (data[k] instanceof Date) {
                //reformat date
                MDC.remove(k + 'Time')
            }
            MDC.remove(k)
        }
        ['id', 'jobName', 'groupPath', 'project'].each {k ->
            MDC.remove(k)
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def void issueJobChangeEvents(Collection<JobChangeEvent> events) {
        events?.each{
            issueJobChangeEvent(it)
        }
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    def void issueJobChangeEvent(JobChangeEvent event) {
        if (event) {
            grailsEvents?.event(null, 'jobChanged', event)
        }
    }
    static def parseNotificationsFromParams(params){
        def notifyParamKeys = ['notifyPlugin']+ ScheduledExecutionController.NOTIFICATION_ENABLE_FIELD_NAMES
        if (!params.notifications && params.subMap(notifyParamKeys).any { it.value }) {
            params.notifications = parseParamNotifications(params)
        }
    }
    static List parseParamNotifications(params){
        List nots=[]
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL]) {
            def config= [
                    recipients: params[ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS],
            ]
            if(params[ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT]){
                config.subject= params[ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT]
            }
            if (params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH]!=null) {
                config.attachLog = params[ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH] in ['true',true]
            }
            nots << [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSUCCESS_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    content: params[ScheduledExecutionController.NOTIFY_SUCCESS_URL]]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT]
            }
            if (params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH]!=null) {
                config.attachLog = params[ScheduledExecutionController.NOTIFY_FAILURE_ATTACH] in ['true', true]
            }
            nots << [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONFAILURE_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    content: params[ScheduledExecutionController.NOTIFY_FAILURE_URL]]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSTART_EMAIL]) {
            def config = [
                    recipients: params[ScheduledExecutionController.NOTIFY_START_RECIPIENTS],
            ]
            if (params[ScheduledExecutionController.NOTIFY_START_SUBJECT]) {
                config.subject = params[ScheduledExecutionController.NOTIFY_START_SUBJECT]
            }
            nots << [eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                    type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                    configuration: config
            ]
        }
        if ('true' == params[ScheduledExecutionController.NOTIFY_ONSTART_URL]) {
            nots << [eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                    type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                    content: params[ScheduledExecutionController.NOTIFY_START_URL]]
        }
        //notifyOnsuccessPlugin
        if (params.notifyPlugin) {
            [ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, ScheduledExecutionController
                    .ONFAILURE_TRIGGER_NAME, ScheduledExecutionController.ONSTART_TRIGGER_NAME].each { trig ->
//                params.notifyPlugin.each { trig, plug ->
                def plugs = params.notifyPlugin[trig]
                if (plugs) {
                    def types = [plugs['type']].flatten()
                    types.each { pluginType ->
                        def config = plugs[pluginType]?.config
                        if (plugs['enabled'][pluginType] == 'true') {
                            nots << [eventTrigger: trig, type: pluginType, configuration: config]
                        }
                    }
                }
            }
        }
        nots
    }

    static def parseOrchestratorFromParams(params){
        
        if (params.orchestratorId) {
            params.orchestrator = parseParamOrchestrator(params)
        }
    }
    
    static Orchestrator parseParamOrchestrator(params){
        Orchestrator orchestrator = new Orchestrator(type:params.orchestratorId)
        def plugin = params.orchestratorPlugin[params.orchestratorId];
        //def config = params.orchestratorPlugin[params.orchestratorId].config
        if(plugin){
            orchestrator.configuration = plugin.config
        }
        orchestrator
    }

    private Map _updateOrchestratorData(params, ScheduledExecution scheduledExecution) {
        //plugin type
        Orchestrator orchestrator = params.orchestrator
        if(scheduledExecution.orchestrator){
            scheduledExecution.orchestrator.discard()
        }
        scheduledExecution.orchestrator = orchestrator
        //TODO:validate inputs
        return [failed:false]
    }

    def _doUpdateExecutionFlags(params, user, String roleList, Framework framework, AuthContext authContext, changeinfo = [:]) {
        log.debug("ScheduledExecutionController: update : attempting to updateExecutionFlags: " + params.id + ". params: " + params)

        def ScheduledExecution scheduledExecution = getByIDorUUID(params.id)
        if (!scheduledExecution) {
            return [success: false]
        }


        def oldSched = scheduledExecution.scheduled
        def oldJobName = scheduledExecution.generateJobScheduledName()
        def oldJobGroup = scheduledExecution.generateJobGroupName()

        if (null != params.scheduleEnabled) {
            if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_SCHEDULE], scheduledExecution.project)) {
                return [success     : false, scheduledExecution: scheduledExecution,
                        message     : lookupMessage(
                                'api.error.item.unauthorized',
                                [AuthConstants.ACTION_TOGGLE_SCHEDULE, 'Job ID', scheduledExecution.extid].toArray()

                        ),
                        errorCode   : 'api.error.item.unauthorized',
                        unauthorized: true]
            }
            if(!scheduledExecution.scheduled){

                return [success: false, scheduledExecution: scheduledExecution,
                        errorCode: 'api.error.job.toggleSchedule.notScheduled' ]
            }
            scheduledExecution.properties.scheduleEnabled = params.scheduleEnabled
        }

        if (null != params.executionEnabled) {
            if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_TOGGLE_EXECUTION], scheduledExecution.project)) {
                return [success          : false, scheduledExecution: scheduledExecution,
                        message          : lookupMessage(
                                'api.error.item.unauthorized',
                                [AuthConstants.ACTION_TOGGLE_EXECUTION, 'Job ID', scheduledExecution.extid].toArray()

                        ),
                        errorCode   : 'api.error.item.unauthorized',
                        unauthorized: true]
            }
            scheduledExecution.properties.executionEnabled = params.executionEnabled
        }

        if (!scheduledExecution.validate()) {
            return [success: false]
        }

        if (scheduledExecution.save(true)) {
            rescheduleJob(scheduledExecution, oldSched, oldJobName, oldJobGroup)
            return [success: true, scheduledExecution: scheduledExecution]
        } else {
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }
    }

    def _doupdate ( params, UserAndRolesAuthContext authContext, changeinfo = [:] ){
        log.debug("ScheduledExecutionController: update : attempting to update: " + params.id +
                  ". params: " + params)
        /**
         * stores info about change for logging purposes
         */
        if (params.groupPath) {
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if (matcher.matches()) {
                params.groupPath = matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            } else {
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        boolean failed = false
        def ScheduledExecution scheduledExecution = getByIDorUUID(params.id)
        if (!scheduledExecution) {
            return [success: false]
        }

        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_UPDATE], scheduledExecution.project)) {
            return [success: false, scheduledExecution: scheduledExecution, message: "Update Job ${scheduledExecution.extid}", unauthorized: true]
        }

        def crontab = [:]
        def oldjobname = scheduledExecution.generateJobScheduledName()
        def oldjobgroup = scheduledExecution.generateJobGroupName()
        def oldsched = scheduledExecution.scheduled
        def nonopts = params.findAll { !it.key.startsWith("option.") && it.key != 'workflow' && it.key != 'options' && it.key != 'notifications'}
        if (scheduledExecution.uuid) {
            nonopts.uuid = scheduledExecution.uuid//don't modify uuid if it exists
        } else if (!nonopts.uuid) {
            //set UUID if not submitted
            nonopts.uuid = UUID.randomUUID().toString()
        }
        if (nonopts.uuid != scheduledExecution.uuid) {
            changeinfo.extraInfo = " (internalID:${scheduledExecution.id})"
        }
        def originalRef = jobEventRevRef(scheduledExecution)

        scheduledExecution.properties = nonopts

        if(!scheduledExecution.nodeThreadcount){
            scheduledExecution.nodeThreadcount=1
        }

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }


        if (!scheduledExecution.validate()) {
            failed = true
        }
        if(originalRef.groupPath!=scheduledExecution.groupPath || originalRef.jobName!=scheduledExecution.jobName){
            //reauthorize if the name/group has changed
            if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_CREATE], scheduledExecution.project)) {
                failed = true
                scheduledExecution.errors.rejectValue('jobName', 'ScheduledExecution.jobName.unauthorized', [AuthConstants.ACTION_CREATE, scheduledExecution.jobName].toArray(), 'Unauthorized action: {0} for value: {1}')
                scheduledExecution.errors.rejectValue('groupPath', 'ScheduledExecution.groupPath.unauthorized', [ AuthConstants.ACTION_CREATE, scheduledExecution.groupPath].toArray(), 'Unauthorized action: {0} for value: {1}')
            }
        }
        if (scheduledExecution.scheduled) {
            scheduledExecution.populateTimeDateFields(params)
            scheduledExecution.user = authContext.username
            scheduledExecution.userRoleList = authContext.roles.join(',')
            if (frameworkService.isClusterModeEnabled()) {
                scheduledExecution.serverNodeUUID = frameworkService.getServerUUID()
            } else {
                scheduledExecution.serverNodeUUID = null
            }
            def genCron = params.crontabString ? params.crontabString : scheduledExecution.generateCrontabExression()
            if (!CronExpression.isValidExpression(genCron)) {
                failed = true;
                scheduledExecution.errors.rejectValue('crontabString',
                        'scheduledExecution.crontabString.invalid.message', [genCron] as Object[], "Invalid: {0}")
            } else {
                //test for valid schedule
                CronExpression c = new CronExpression(genCron)
                def next = c.getNextValidTimeAfter(new Date());
                if (!next) {
                    failed = true;
                    scheduledExecution.errors.rejectValue('crontabString',
                            'scheduledExecution.crontabString.noschedule.message', [genCron] as Object[],
                            "Invalid: {0}")
                }
            }
        } else {
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution = new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        def boolean renamed = oldjobname != scheduledExecution.generateJobScheduledName() || oldjobgroup != scheduledExecution.generateJobGroupName()
        if (renamed) {
            changeinfo.rename = true
            changeinfo.origName = oldjobname
            changeinfo.origGroup = oldjobgroup
        }


        if (!frameworkService.existsFrameworkProject(scheduledExecution.project)) {
            failed = true
            scheduledExecution.errors.rejectValue('project', 'scheduledExecution.project.invalid.message', [scheduledExecution.project].toArray(), 'Project was not found: {0}')
        }
        def frameworkProject = frameworkService.getFrameworkProject(scheduledExecution.project)
        def projectProps = frameworkProject.getProperties()

        def todiscard = []
        def wftodelete = []
        if (scheduledExecution.workflow && params['_sessionwf'] && params['_sessionEditWFObject']) {
            //load the session-stored modified workflow and replace the existing one
            def Workflow wf = params['_sessionEditWFObject']//session.editWF[scheduledExecution.id.toString()]
            if (!wf.commands || wf.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            } else {
                def wfitemfailed = false
                def failedlist = []
                def i = 1;
                wf.commands.each {WorkflowStep cexec ->
                    WorkflowController._validateCommandExec(cexec)
                    if (cexec.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << i
                    }
                    if (cexec.errorHandler) {
                        WorkflowController._validateCommandExec(cexec.errorHandler)
                        if (cexec.errorHandler.errors.hasErrors()) {
                            wfitemfailed = true
                            failedlist << (i + 1)
                        }
                    }
                    i++
                }
                if (!wfitemfailed) {
                    def oldwf = scheduledExecution.workflow
                    final Workflow newworkflow = new Workflow(wf)
                    scheduledExecution.workflow = newworkflow
                    if (oldwf) {
                        wftodelete << oldwf
                    }
                    todiscard<<wf
                } else {
                    failed = true
                    scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
                }

            }
        } else if (params.workflow && params['_workflow_data']) {
            //use the input params to define the workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount: params.workflow.threadcount ? params.workflow.threadcount : 1, keepgoing: null != params.workflow.keepgoing ? params.workflow.keepgoing : false, scheduledExecution: scheduledExecution)
            def i = 0;
            def wfitemfailed = false
            def failedlist = []
            while (params.workflow["commands[${i}]"]) {
                def Map cmdparams = params.workflow["commands[${i}]"]
                def cexec
                if (cmdparams.jobName) {
                    cexec = new JobExec()
                } else {
                    //TODO
                    cexec = new CommandExec()
                }
                cexec.properties = cmdparams
                workflow.addToCommands(cexec)
                WorkflowController._validateCommandExec(cexec)
                if (cexec.errors.hasErrors()) {
                    wfitemfailed = true
                    failedlist << (i + 1)
                }
                if (cmdparams.errorHandler) {
                    WorkflowController._validateCommandExec(cmdparams.errorHandler)
                    if (cmdparams.errorHandler.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << (i + 1)
                    }
                }
                i++
            }
            scheduledExecution.workflow = workflow

            if (wfitemfailed) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
            }
            if (!workflow.commands || workflow.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            }
        } else if (!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size() < 1) {
            failed = true
            scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
        }
        if(scheduledExecution.workflow) {
            if(params.workflow && null!=params.workflow.keepgoing) {
                scheduledExecution.workflow.keepgoing = params.workflow.keepgoing in ['true', true]
            }
            if(params.workflow && null!=params.workflow.strategy){
                scheduledExecution.workflow.strategy = params.workflow.strategy
            }
        }

        //validate error handler types
        if (!validateWorkflow(scheduledExecution.workflow,scheduledExecution)) {
            failed = true
        }
        if (( params.options || params['_nooptions']) && scheduledExecution.options) {
            def todelete = []
            scheduledExecution.options.each {
                todelete << it
            }
            todelete.each {
                it.delete()
                scheduledExecution.removeFromOptions(it)
            }
            scheduledExecution.options = null
        }
        if (params['_sessionopts'] && null!=params['_sessionEditOPTSObject']) {
            def optsmap = params['_sessionEditOPTSObject']

            def optfailed = false
            optsmap.values().each {Option opt ->
                EditOptsController._validateOption(opt,null,scheduledExecution.scheduled)
                if (opt.errors.hasErrors()) {
                    optfailed = true
                    def errmsg = opt.name + ": " + opt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                            'options',
                            'scheduledExecution.options.invalid.message',
                            [errmsg] as Object[],
                            'Invalid Option definition: {0}'
                    )
                }
            }
            if (!optfailed) {
                def todelete = []
                if(scheduledExecution.options){
                    todelete.addAll(scheduledExecution.options)
                }
                scheduledExecution.options = null
                todelete.each {oldopt ->
                    oldopt.delete()
                }
                optsmap.values().each {Option opt ->
                    opt.convertValuesList()
                    Option newopt = opt.createClone()
                    scheduledExecution.addToOptions(newopt)
                }
            } else {
                failed = true
            }
        } else if (params.options) {

            //set user options:
            def i = 0;
            while (params.options["options[${i}]"]) {
                def Map optdefparams = params.options["options[${i}]"]
                def Option theopt = new Option(optdefparams)
                scheduledExecution.addToOptions(theopt)
                EditOptsController._validateOption(theopt,null,scheduledExecution.scheduled)
                if (theopt.errors.hasErrors() || !theopt.validate()) {
                    failed = true
                    theopt.discard()
                    def errmsg = optdefparams.name + ": " + theopt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                            'options',
                            'scheduledExecution.options.invalid.message',
                            [errmsg] as Object[],
                            'Invalid Option definition: {0}'
                    )
                }
                theopt.scheduledExecution = scheduledExecution
                i++
            }

        }else if(scheduledExecution.options && scheduledExecution.scheduled){
            //evaluate required option defaults
            scheduledExecution.options.each{Option theopt->
                EditOptsController._validateOption(theopt,null,scheduledExecution.scheduled)
                if(theopt.errors.hasErrors()) {
                    failed=true
                    def errmsg = theopt.name + ": " +
                            theopt.errors.allErrors.collect { lookupMessageError(it) }.join(";")

                    scheduledExecution.errors.rejectValue(
                            'options',
                            'scheduledExecution.options.invalid.message',
                            [errmsg] as Object[],
                            'Invalid Option definition: {0}'
                    )
                }
            }

        }
        
        parseOrchestratorFromParams(params)
        if(params.orchestrator){
            def result = _updateOrchestratorData(params, scheduledExecution)
            scheduledExecution.orchestrator.save()
            if (result.failed) {
                failed = result.failed
            }
        }else{
            scheduledExecution.orchestrator = null
        }

        parseNotificationsFromParams(params)
        if (!params.notifications) {
            params.notified = 'false'
        }
        def modifiednotifs = []
        if (params.notifications && 'false' != params.notified) {
            //create notifications
            def result = _updateNotificationsData(params, scheduledExecution,projectProps)
            if(result.failed){
                failed = result.failed
            }
            modifiednotifs=result.modified
        }
        //delete notifications that are not part of the modified set
        if (scheduledExecution.notifications) {
            def todelete = []
            scheduledExecution.notifications.each { Notification note ->
                if (!(note in modifiednotifs)) {
                    todelete << note
                }
            }

            if(!failed){
                todelete.each {
                    it.delete()
                    scheduledExecution.removeFromNotifications(it)
                    todiscard << it
                }
            }
        }

        //try to save workflow
        if (!failed && null != scheduledExecution.workflow) {
            if (!scheduledExecution.workflow.validate()) {
                log.error("unable to save workflow: " + scheduledExecution.workflow.errors.allErrors.collect {lookupMessageError(it)}.join("\n"))
                failed = true;
            } else {
                scheduledExecution.workflow.save(flush: true)
                wftodelete.each{it.delete()}
            }
        }else if (failed && null!=scheduledExecution.workflow){
            todiscard<< scheduledExecution.workflow
        }
        if (!failed) {
            if (!scheduledExecution.validate()) {
                failed = true
            }
        }
        if (!failed && scheduledExecution.save(true)) {
            if (scheduledExecution.shouldScheduleExecution()) {
                def nextdate = null
                try {
                    nextdate = scheduleJob(scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null);
                } catch (SchedulerException e) {
                    log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution = nextdate
                if (!newsched.save()) {
                    log.error("Unable to save second change to scheduledExec.")
                }
            } else if (oldsched && oldjobname && oldjobgroup) {
                deleteJob(oldjobname, oldjobgroup)
            }
            def eventType=JobChangeEvent.JobChangeEventType.MODIFY
            if (originalRef.jobName != scheduledExecution.jobName || originalRef.groupPath != scheduledExecution.groupPath) {
                eventType = JobChangeEvent.JobChangeEventType.MODIFY_RENAME
            }

            def event = createJobChangeEvent(eventType, scheduledExecution, originalRef)
            return [success: true, scheduledExecution: scheduledExecution,jobChangeEvent:event]
        } else {
            todiscard.each {
                it.discard()
            }
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }

    }

    private Map validatePluginNotification(ScheduledExecution scheduledExecution, String trigger,notif,params=null, Map projectProperties=null){
        //plugin type
        def failed=false
        def pluginDesc = notificationService.getNotificationPluginDescriptor(notif.type)
        if (!pluginDesc) {
            scheduledExecution.errors.rejectValue(
                    'notifications',
                    'scheduledExecution.notifications.pluginTypeNotFound.message',
                    [notif.type] as Object[],
                    'Notification Plugin type "{0}" was not found or could not be loaded'
            )
            return [failed:true]
        }
        def validation = notificationService.validatePluginConfig(notif.type, projectProperties, notif.configuration)
        if (!validation.valid) {
            failed = true
            if(params instanceof Map){
                if (!params['notificationValidation']) {
                    params['notificationValidation'] = [:]
                }
                if (!params['notificationValidation'][trigger]) {
                    params['notificationValidation'][trigger] = [:]
                }
                params['notificationValidation'][trigger][notif.type] = validation
            }
            scheduledExecution.errors.rejectValue(
                    'notifications',
                    'scheduledExecution.notifications.invalidPlugin.message',
                    [notif.type] as Object[],
                    'Invalid Configuration for plugin: {0}'
            )
        }
        if (failed) {
            return [failed:true]
        }
        //TODO: better config test
        def n = Notification.fromMap(trigger, notif)
        [failed:failed,notification:n]
    }
    private Map validateEmailNotification(ScheduledExecution scheduledExecution, String trigger, notif, params = null){
        def failed
        def fieldNames = [
                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS,
                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS,
                (ScheduledExecutionController.ONSTART_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_START_RECIPIENTS
        ]
        def conf = notif.configuration
        def arr = (conf?.recipients?: notif.content)?.split(",")
        def validator = new AnyDomainEmailValidator()
        def validcount=0
        arr?.each { email ->
            if(email && email.indexOf('${')>=0){
                //don't reject embedded prop refs
                validcount++
            }else if (email && !validator.isValid(email)) {
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalidemail.message',
                        [email] as Object[],
                        'Invalid email address: {0}'
                )
            }else if(email){
                validcount++
            }
        }
        if(!failed && validcount<1){
            failed=true
            scheduledExecution.errors.rejectValue(
                    fieldNames[trigger],
                    'scheduledExecution.notifications.email.blank.message',
                    'Cannot be blank'
            )
        }
        if (failed) {
            return [failed:true]
        }
        def addrs = arr.findAll { it.trim() }.join(",")
        def configuration=[:]
        if(conf){
            configuration = conf + [recipients: addrs]
        }else{
            configuration.recipients = addrs
        }
        def n = Notification.fromMap(trigger, [email: configuration])
        [failed: false, notification: n]
    }
    private Map validateUrlNotification(ScheduledExecution scheduledExecution, String trigger, notif, params = null){
        def failed
        def fieldNamesUrl = [
                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_SUCCESS_URL,
                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_FAILURE_URL,
                (ScheduledExecutionController.ONSTART_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_START_URL
        ]
        def arr = notif.content.split(",")
        def validCount=0
        arr.each { String url ->
            boolean valid = false
            try {
                new URL(url)
                valid = true
            } catch (MalformedURLException e) {
                valid = false
            }
            if (url && !valid) {
                failed = true
                scheduledExecution.errors.rejectValue(
                        fieldNamesUrl[trigger],
                        'scheduledExecution.notifications.invalidurl.message',
                        [url] as Object[],
                        'Invalid URL: {0}'
                )
            }else if(url && valid){
                validCount++
            }
        }
        if(validCount<1){
            failed = true
            scheduledExecution.errors.rejectValue(
                    fieldNamesUrl[trigger],
                    'scheduledExecution.notifications.url.blank.message',
                    'Webhook URL cannot be blank'
            )
        }
        if (failed) {
            return [failed: true]
        }
        def addrs = arr.findAll { it.trim() }.join(",")
        def n = new Notification(eventTrigger: trigger, type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE, content: addrs)
        [failed:false,notification: n]
    }

    /**
     * Update ScheduledExecution notification definitions based on input params.
     *
     * expected params: [notifications: [<eventTrigger>:[email:<content>]]]
     */
    private Map _updateNotificationsData( params, ScheduledExecution scheduledExecution, Map projectProperties) {
        boolean failed = false
        def fieldNames = [
                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS,
                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS,
                (ScheduledExecutionController.ONSTART_TRIGGER_NAME):
                        ScheduledExecutionController.NOTIFY_START_RECIPIENTS
        ]
        def fieldNamesUrl = [
                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_SUCCESS_URL,
                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_FAILURE_URL,
                (ScheduledExecutionController.ONSTART_TRIGGER_NAME): ScheduledExecutionController.NOTIFY_START_URL,
        ]
        
        def addedNotifications=[]
        params.notifications.each {notif ->
            def trigger = notif.eventTrigger
            def Notification n
            def String failureField
            if (notif && notif.type == ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE ) {
                def result=validateEmailNotification(scheduledExecution,trigger,notif,params)
                if(result.failed){
                    failed=true
                }else{
                    n=result.notification
                }
                failureField= fieldNames[trigger]
            } else if (notif && notif.type == ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE ) {

                def result = validateUrlNotification(scheduledExecution, trigger, notif, params)
                if (result.failed) {
                    failed = true
                } else {
                    n = result.notification
                }
                failureField = fieldNamesUrl[trigger]
            } else if (notif.type) {
                def data=notif
                if(notif instanceof Notification){
                    data=[type:notif.type, configuration:notif.configuration]
                }
                def result = validatePluginNotification(scheduledExecution, trigger, data, params,projectProperties)
                if (result.failed) {
                    failed = true
                    failureField="notifications"
                } else {
                    n = result.notification
                }
            }
            if(n){
                //modify existing notification
                def oldn = scheduledExecution.findNotification(n.eventTrigger,n.type)
                if(oldn){
                    oldn.content=n.content
                    n=oldn
                    n.scheduledExecution = scheduledExecution
                }else{
                    n.scheduledExecution = scheduledExecution
                }
                if (!n.validate()) {
                    failed = true
                    n.discard()
                    def errmsg = trigger + " notification: " + n.errors.allErrors.collect { lookupMessageError(it) }.join(";")
                    scheduledExecution.errors.rejectValue(
                            failureField,
                            'scheduledExecution.notifications.invalid.message',
                            [errmsg] as Object[],
                            'Invalid notification definition: {0}'
                    )
                    scheduledExecution.discard()
                }else{
                    if(!oldn){
                        scheduledExecution.addToNotifications(n)
                    }
                    addedNotifications << n
                }
            }
        }
        return [failed:failed,modified:addedNotifications]
    }
    public Map _doupdateJob(id, ScheduledExecution params, UserAndRolesAuthContext authContext, changeinfo = [:]) {
        log.debug("ScheduledExecutionController: update : attempting to update: " + id +
                  ". params: " + params)
        if (params.groupPath) {
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if (matcher.matches()) {
                params.groupPath = matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            } else {
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        boolean failed = false
        def ScheduledExecution scheduledExecution = ScheduledExecution.get(id)

        def crontab = [:]
        if (!scheduledExecution) {
            return [success:false]
        }
        def oldjobname = scheduledExecution.generateJobScheduledName()
        def oldjobgroup = scheduledExecution.generateJobGroupName()
        def oldsched = scheduledExecution.scheduled
        scheduledExecution.properties = null
        final Collection foundprops = params.properties.keySet().findAll {it != 'lastUpdated' && it != 'dateCreated' && (params.properties[it] instanceof String || params.properties[it] instanceof Boolean || params.properties[it] instanceof Integer) }
        final Map newprops = foundprops ? params.properties.subMap(foundprops) : [:]
        if (scheduledExecution.uuid) {
            newprops.uuid = scheduledExecution.uuid//don't modify uuid if it exists
        } else if (!newprops.uuid) {
            //set UUID if not submitted
            newprops.uuid = UUID.randomUUID().toString()
        }
        if (newprops.uuid != scheduledExecution.uuid) {
            changeinfo.extraInfo = " (internalID:${scheduledExecution.id})"
        }
        //clear filter params
        scheduledExecution.clearFilterFields()
        def originalRef=jobEventRevRef(scheduledExecution)

        scheduledExecution.properties = newprops

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }

        if (!scheduledExecution.validate()) {
            failed = true
        }

        if (originalRef.groupPath != scheduledExecution.groupPath || originalRef.jobName != scheduledExecution.jobName) {
            //reauthorize if the name/group has changed
            if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_CREATE], scheduledExecution.project)) {
                failed = true
                scheduledExecution.errors.rejectValue('jobName', 'ScheduledExecution.jobName.unauthorized', [AuthConstants.ACTION_CREATE, scheduledExecution.jobName].toArray(), 'Unauthorized action: {0} for value: {1}')
                scheduledExecution.errors.rejectValue('groupPath', 'ScheduledExecution.groupPath.unauthorized', [AuthConstants.ACTION_CREATE, scheduledExecution.groupPath].toArray(), 'Unauthorized action: {0} for value: {1}')
            }
        }
        if (scheduledExecution.scheduled) {
            scheduledExecution.user = authContext.username
            scheduledExecution.userRoleList = authContext.roles.join(",")
            if (frameworkService.isClusterModeEnabled()) {
                scheduledExecution.serverNodeUUID = frameworkService.getServerUUID()
            } else {
                scheduledExecution.serverNodeUUID = null
            }

            if (scheduledExecution.crontabString && (!CronExpression.isValidExpression(scheduledExecution.crontabString)
                    ||                               !scheduledExecution.parseCrontabString(scheduledExecution.crontabString))) {
                failed = true;
                scheduledExecution.errors.rejectValue('crontabString', 'scheduledExecution.crontabString.invalid.message')
            }
            def genCron = scheduledExecution.generateCrontabExression()
            if (!CronExpression.isValidExpression(genCron)) {
                failed = true;
                scheduledExecution.errors.rejectValue('crontabString',
                        'scheduledExecution.crontabString.invalid.message',[genCron] as Object[],"invalid: {0}")
            } else {
                //test for valid schedule
                CronExpression c = new CronExpression(genCron)
                def next = c.getNextValidTimeAfter(new Date());
                if (!next) {
                    failed = true;
                    scheduledExecution.errors.rejectValue('crontabString',
                            'scheduledExecution.crontabString.noschedule.message', [genCron] as Object[], "invalid: {0}")
                }
            }
        } else {
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution = new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        def boolean renamed = oldjobname != scheduledExecution.generateJobScheduledName() || oldjobgroup != scheduledExecution.generateJobGroupName()


        if (scheduledExecution.project && !frameworkService.existsFrameworkProject(scheduledExecution.project)) {
            failed = true
            scheduledExecution.errors.rejectValue('project', 'scheduledExecution.project.invalid.message', [scheduledExecution.project].toArray(), 'Project was not found: {0}')
        }
        def frameworkProject = frameworkService.getFrameworkProject(scheduledExecution.project)
        def projectProps = frameworkProject.getProperties()

        if (params.workflow) {
            //use the input params to define the workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(params.workflow)
            def i = 0;
            def wfitemfailed = false
            def failedlist = []
            workflow.commands.each {WorkflowStep cmdparams ->
                WorkflowController._validateCommandExec(cmdparams)
                if (cmdparams.errors.hasErrors()) {
                    wfitemfailed = true
                    failedlist << (i + 1)
                }
                if (cmdparams.errorHandler) {
                    WorkflowController._validateCommandExec(cmdparams.errorHandler)
                    if (cmdparams.errorHandler.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << (i + 1)
                    }
                }
                i++
            }
            scheduledExecution.workflow = workflow

            if (wfitemfailed) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
            }
            if (!workflow.commands || workflow.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            }
        } else if (!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size() < 1) {
            failed = true
            scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
        }

        //validate error handler types
        if (!validateWorkflow(scheduledExecution.workflow,scheduledExecution)) {
            failed = true
        }
        if (scheduledExecution.options) {
            def todelete = []
            scheduledExecution.options.each {
                todelete << it
            }
            todelete.each {
                it.delete()
                scheduledExecution.removeFromOptions(it)
            }
            scheduledExecution.options = null
        }
        if (params.options) {

            //set user options:
            def i = 0;
            params.options.each {Option theopt ->
                scheduledExecution.addToOptions(theopt)
                EditOptsController._validateOption(theopt,null,scheduledExecution.scheduled)
                if (theopt.errors.hasErrors() || !theopt.validate()) {
                    failed = true
                    theopt.discard()
                    def errmsg = theopt.name + ": " + theopt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                            'options',
                            'scheduledExecution.options.invalid.message',
                            [errmsg] as Object[],
                            'Invalid Option definition: {0}'
                    )
                }
                theopt.scheduledExecution = scheduledExecution
                i++
            }

        }
        
        if(params.orchestrator){
            def result = _updateOrchestratorData(params, scheduledExecution)
            scheduledExecution.orchestrator.save()
            if (result.failed) {
                failed = result.failed
            }
        }else{
            scheduledExecution.orchestrator = null
        }

        def todiscard = []
        def modifiednotifs=[]
        if (params.notifications) {
            //create notifications
            def result = _updateNotificationsData(params, scheduledExecution,projectProps)
            if (result.failed) {
                failed = result.failed
            }
            modifiednotifs=result.modified
        }

        //delete notifications that are not part of the modified set
        if (scheduledExecution.notifications) {
            def todelete = []
            scheduledExecution.notifications.each { Notification note ->
                if(!(note in modifiednotifs)){
                    todelete << note
                }
            }
            if(!failed){
                todelete.each {
                    it.delete()
                    scheduledExecution.removeFromNotifications(it)
                    todiscard << it
                }
            }
        }

        //try to save workflow
        if (!failed && null != scheduledExecution.workflow) {
            if (!scheduledExecution.workflow.validate()) {
                log.error("unable to save workflow: " + scheduledExecution.workflow.errors.allErrors.collect {lookupMessageError(it)}.join("\n"))
                failed = true;
            } else {
                scheduledExecution.workflow.save(flush: true)
            }
        }
        if (!failed) {
            if (!scheduledExecution.validate()) {
                failed = true
            }
        }

        if (!failed && scheduledExecution.save(flush:true)) {
            if (scheduledExecution.shouldScheduleExecution()) {
                def nextdate = null
                try {
                    nextdate = scheduleJob(scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null);
                } catch (SchedulerException e) {
                    log.error("Unable to schedule job: ${scheduledExecution.extid}: ${e.message}")
                }
                def newsched = ScheduledExecution.get(scheduledExecution.id)
                newsched.nextExecution = nextdate
                if (!newsched.save()) {
                    log.error("Unable to save second change to scheduledExec.")
                }
            } else if (oldsched && oldjobname && oldjobgroup) {
                deleteJob(oldjobname, oldjobgroup)
            }
            def eventType=JobChangeEvent.JobChangeEventType.MODIFY
            if(originalRef.jobName!=scheduledExecution.jobName || originalRef.groupPath!=scheduledExecution.groupPath){
                eventType=JobChangeEvent.JobChangeEventType.MODIFY_RENAME
            }

            def event = createJobChangeEvent (eventType, scheduledExecution, originalRef)

            return [success:true, scheduledExecution:  scheduledExecution,jobChangeEvent: event]
        } else {
            todiscard.each {
                it.discard()
            }
            scheduledExecution.discard()
            return [success:false, scheduledExecution:  scheduledExecution]
        }

    }

    public Map _dosave(params, UserAndRolesAuthContext authContext, changeinfo = [:]) {
        log.debug("ScheduledExecutionController: save : params: " + params)
        boolean failed = false;
        if (params.groupPath) {
            def re = /^\/*(.+?)\/*$/
            def matcher = params.groupPath =~ re
            if (matcher.matches()) {
                params.groupPath = matcher.group(1);
                log.debug("params.groupPath updated: ${params.groupPath}")
            } else {
                log.debug("params.groupPath doesn't match: ${params.groupPath}")
            }
        }
        def map
        if(params instanceof ScheduledExecution){
            map=new HashMap(params.properties)
            if(params.scheduled){
                map.crontabString=params.generateCrontabExression()
                map.useCrontabString='true'
            }
        } else{
            map=params
        }
        def result = _dovalidate(map, authContext)
        def scheduledExecution = result.scheduledExecution
        failed = result.failed
        //try to save workflow
        if(failed){
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }
        if (!frameworkService.authorizeProjectJobAll(authContext, scheduledExecution, [AuthConstants.ACTION_CREATE], scheduledExecution.project)) {
            scheduledExecution.discard()
            return [success: false, error: "Unauthorized: Create Job ${scheduledExecution.generateFullName()}", unauthorized: true, scheduledExecution: scheduledExecution]
        }
        if (!failed && null != scheduledExecution.workflow) {
            if (!scheduledExecution.workflow.save(flush: true)) {
                log.error(scheduledExecution.workflow.errors.allErrors.collect {lookupMessageError(it)}.join("\n"))
                failed = true;
            }
        }

        //set UUID if not submitted
        if (!scheduledExecution.uuid) {
            scheduledExecution.uuid = UUID.randomUUID().toString()
        }
        if (!failed && scheduledExecution.save(flush:true)) {
            rescheduleJob(scheduledExecution)
            def event = createJobChangeEvent(JobChangeEvent.JobChangeEventType.CREATE, scheduledExecution)
            return [success: true, scheduledExecution: scheduledExecution,jobChangeEvent: event]

        } else {
            scheduledExecution.discard()
            return [success: false, scheduledExecution: scheduledExecution]
        }
    }

    private static StoredJobChangeEvent createJobChangeEvent(
            JobChangeEvent.JobChangeEventType type,
            ScheduledExecution scheduledExecution,
            JobReference orig = null
    )
    {
        createJobChangeEvent(type, jobEventRevRef(scheduledExecution), orig)
    }
    private static StoredJobChangeEvent createJobChangeEvent(
            JobChangeEvent.JobChangeEventType type,
            JobRevReference rev,
            JobReference orig = null
    )
    {
        new StoredJobChangeEvent(
                eventType: type,
                originalJobReference: orig?:rev,
                jobReference: rev

        )
    }

    private static JobRevReferenceImpl jobEventRevRef(ScheduledExecution scheduledExecution) {
        new JobRevReferenceImpl(
                id: scheduledExecution.extid,
                jobName: scheduledExecution.jobName,
                groupPath: scheduledExecution.groupPath,
                project: scheduledExecution.project,
                version: scheduledExecution.version
        )
    }

    /**
     * Parse some kind of job input request using the specified format
     * @param input either an inputStream, a File, or a String
     */
    def parseUploadedFile (input, fileformat){
        def jobset
        if ('xml' == fileformat) {
            try {
                jobset = input.decodeJobsXML()
            } catch (JobXMLException e) {
                log.error("Error parsing upload Job XML: ${e}")
                log.warn("Error parsing upload Job XML", e)
                return [error: "${e}"]
            } catch (Exception e) {
                log.error("Error parsing upload Job XML", e)
                return [error: "${e}"]
            }
        } else if ('yaml' == fileformat) {

            try {
                //load file into string
                jobset = input.decodeJobsYAML()
            } catch (JobXMLException e) {
                log.error("Error parsing upload Job Yaml: ${e}")
                log.warn("Error parsing upload Job Yaml", e)
                return [error: "${e}"]
            } catch (Exception e) {
                log.error("Error parsing upload Job Yaml", e)
                return [error: "${e}"]
            }
        } else {
            return [errorCode: 'api.error.jobs.import.format.unsupported', args: [fileformat]]
        }
        if (null == jobset) {
            return [errorCode: 'api.error.jobs.import.empty']
        }
        return [jobset: jobset]
    }
    /**
     * Validate workflow command error handler types, return true if valid
     * @param workflow
     * @param scheduledExecution
     * @return
     */
    def boolean validateWorkflow(Workflow workflow, ScheduledExecution scheduledExecution){
        def valid=true
        //validate error handler types
        if (workflow?.strategy == 'node-first') {
            //if a step is a Node step and has an error handler
            def cmdi = 1;
            workflow.commands.each { WorkflowStep step ->
                if(step.errorHandler && step.nodeStep && !step.errorHandler.nodeStep){
                    //reject if the Error Handler is not a node step
                    step.errors.rejectValue('errorHandler', 'WorkflowStep.errorHandler.nodeStep.invalid', [cmdi] as Object[], "Step {0}: Must have a Node Step as an Error Handler")
                    scheduledExecution?.errors.rejectValue('workflow', 'Workflow.stepErrorHandler.nodeStep.invalid', [cmdi] as Object[], "Step {0}: Must have a Node Step as an Error Handler")
                    valid = false
                }
                cmdi++
            }
        }
        return valid
    }

    def _dovalidate (Map params, UserAndRoles userAndRoles ){
        log.debug("ScheduledExecutionController: save : params: " + params)
        boolean failed = false;
        def scheduledExecution = new ScheduledExecution()
        final Map nonopts = params.findAll {!it.key.startsWith("option.") && it.key != 'workflow' && it.key != 'options' && it.key != 'notifications'}
        scheduledExecution.properties = nonopts
        if(scheduledExecution.doNodedispatch && !scheduledExecution.filter){
            scheduledExecution.filter=scheduledExecution.asFilter()
        }

        //fix potential null/blank issue after upgrading rundeck to 1.3.1/1.4
        if (!scheduledExecution.description) {
            scheduledExecution.description = ''
        }


        def valid = scheduledExecution.validate()
        if (scheduledExecution.scheduled) {
            scheduledExecution.user = userAndRoles.username
            scheduledExecution.userRoleList = userAndRoles.roles.join(',')
            if (frameworkService.isClusterModeEnabled()) {
                scheduledExecution.serverNodeUUID = frameworkService.getServerUUID()
            }else{
                scheduledExecution.serverNodeUUID = null
            }

            scheduledExecution.populateTimeDateFields(params)

            def genCron = params.crontabString ? params.crontabString : scheduledExecution.generateCrontabExression()
            if (!CronExpression.isValidExpression(genCron)) {
                failed = true;
                scheduledExecution.errors.rejectValue('crontabString',
                        'scheduledExecution.crontabString.invalid.message', [genCron] as Object[], "invalid: {0}")
            } else {
                //test for valid schedule
                CronExpression c = new CronExpression(genCron)
                def next = c.getNextValidTimeAfter(new Date());
                if (!next) {
                    failed = true;
                    scheduledExecution.errors.rejectValue('crontabString',
                            'scheduledExecution.crontabString.noschedule.message', [genCron] as Object[], "invalid: {0}")
                }
            }
        } else {
            //set nextExecution of non-scheduled job to be far in the future so that query results can sort correctly
            scheduledExecution.nextExecution = new Date(ScheduledExecutionService.TWO_HUNDRED_YEARS)
        }

        if (scheduledExecution.project && !frameworkService.existsFrameworkProject(scheduledExecution.project)) {
            failed = true
            scheduledExecution.errors.rejectValue('project', 'scheduledExecution.project.invalid.message', [scheduledExecution.project].toArray(), 'Project does not exist: {0}')
        }

        def frameworkProject = frameworkService.getFrameworkProject(scheduledExecution.project)
        def projectProps = frameworkProject.getProperties()

        if (params['_sessionwf'] == 'true' && params['_sessionEditWFObject']) {
            //use session-stored workflow
            def Workflow wf = params['_sessionEditWFObject']
            wf.keepgoing = params.workflow.keepgoing == 'true'
            wf.strategy = params.workflow.strategy
            if (!wf.commands || wf.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            } else {

                def wfitemfailed = false
                def i = 1
                def failedlist = []
                wf.commands.each {WorkflowStep cexec ->
                    WorkflowController._validateCommandExec(cexec)
                    if (cexec.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << i
                    }

                    if (cexec.errorHandler) {
                        WorkflowController._validateCommandExec(cexec.errorHandler)
                        if (cexec.errorHandler.errors.hasErrors()) {
                            wfitemfailed = true
                            failedlist << (i + 1)
                        }
                    }
                    i++
                }
                if (!wfitemfailed) {
                    final Workflow workflow = new Workflow(wf)
                    scheduledExecution.workflow = workflow
                    wf.discard()
                } else {
                    failed = true
                    scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
                }
            }
        } else if (params.workflow && params.workflow instanceof Workflow) {
            def Workflow workflow = new Workflow(params.workflow)
            def i = 0;
            def wfitemfailed = false
            def failedlist = []
            workflow.commands.each {WorkflowStep cmdparams ->
                WorkflowController._validateCommandExec(cmdparams)
                if (cmdparams.errors.hasErrors()) {
                    wfitemfailed = true
                    failedlist << (i + 1)
                }
                if (cmdparams.errorHandler) {
                    WorkflowController._validateCommandExec(cmdparams.errorHandler)
                    if (cmdparams.errorHandler.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << (i + 1)
                    }
                }
                i++
            }
            scheduledExecution.workflow = workflow

            if (wfitemfailed) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
            }
            if (!workflow.commands || workflow.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            }
        } else if (params.workflow) {
            //use input parameters to define workflow
            //create workflow and CommandExecs
            def Workflow workflow = new Workflow(threadcount: params.workflow.threadcount, keepgoing: params.workflow.keepgoing, scheduledExecution: scheduledExecution)
            def i = 0;
            def wfitemfailed = false
            def failedlist = []
            while (params.workflow["commands[${i}]"]) {
                def Map cmdparams = params.workflow["commands[${i}]"]
                def cexec
                if (cmdparams.jobName) {
                    cexec = new JobExec()
                } else {
                    //TODO
                    cexec = new CommandExec()
                }

                if (!cmdparams.project) {
                    cmdparams.project = scheduledExecution.project
                }
                cexec.properties = cmdparams
                workflow.addToCommands(cexec)
                WorkflowController._validateCommandExec(cexec)
                if (cexec.errors.hasErrors()) {
                    wfitemfailed = true
                    failedlist << (i + 1)
                }
                if (cmdparams.errorHandler) {
                    WorkflowController._validateCommandExec(cmdparams.errorHandler)
                    if (cmdparams.errorHandler.errors.hasErrors()) {
                        wfitemfailed = true
                        failedlist << (i + 1)
                    }
                }
                i++
            }
            scheduledExecution.workflow = workflow

            if (wfitemfailed) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.invalidstepslist.message', [failedlist.toString()].toArray(), "Invalid workflow steps: {0}")
            }
            if (!workflow.commands || workflow.commands.size() < 1) {
                failed = true
                scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
            }
        } else if (!scheduledExecution.workflow || !scheduledExecution.workflow.commands || scheduledExecution.workflow.commands.size() < 1) {
            failed = true
            scheduledExecution.errors.rejectValue('workflow', 'scheduledExecution.workflow.empty.message')
        }

        //validate error handler types
        if(!validateWorkflow(scheduledExecution.workflow,scheduledExecution)){
            failed = true
        }

        if (scheduledExecution.argString) {
            try {
                scheduledExecution.argString.replaceAll(/\$\{DATE:(.*)\}/, { all, tstamp ->
                    new SimpleDateFormat(tstamp).format(new Date())
                })
            } catch (IllegalArgumentException e) {
                failed = true;
                scheduledExecution.errors.rejectValue('argString', 'scheduledExecution.argString.datestamp.invalid', [e.getMessage()].toArray(), 'datestamp format is invalid: {0}')
                log.error(e)
            }
        }

        if (params['_sessionopts'] && null!=params['_sessionEditOPTSObject']) {
            def optsmap = params['_sessionEditOPTSObject']

            def optfailed = false
            optsmap.values().each {Option opt ->
                EditOptsController._validateOption(opt,null,scheduledExecution.scheduled)
                if (opt.errors.hasErrors()) {
                    optfailed = true
                    def errmsg = opt.name + ": " + opt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                    scheduledExecution.errors.rejectValue(
                            'options',
                            'scheduledExecution.options.invalid.message',
                            ['Option '+opt.name+': '+errmsg] as Object[],
                            'Invalid Option definition: {0}'
                    )
                }
            }
            if (!optfailed) {
                optsmap.values().each {Option opt ->
                    opt.convertValuesList()
                    Option newopt = opt.createClone()
                    scheduledExecution.addToOptions(newopt)
                }
            } else {
                failed = true
//                scheduledExecution.errors.rejectValue('options', 'scheduledExecution.options.invalid.message')
            }
        } else if (params.options) {
            //set user options:
            def i = 0;
            if (params.options instanceof Collection) {
                params.options.each { origopt ->
                    def Option theopt = origopt.createClone()
                    scheduledExecution.addToOptions(theopt)
                    EditOptsController._validateOption(theopt,null,scheduledExecution.scheduled)

                    if (theopt.errors.hasErrors() || !theopt.validate()) {
                        failed = true
                        theopt.discard()
                        def errmsg = theopt.name + ": " + theopt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                        scheduledExecution.errors.rejectValue(
                                'options',
                                'scheduledExecution.options.invalid.message',
                                [errmsg] as Object[],
                                'Invalid Option definition: {0}'
                        )
                    }
                    i++
                }
            } else if (params.options instanceof Map) {
                while (params.options["options[${i}]"]) {
                    def Map optdefparams = params.options["options[${i}]"]
                    def Option theopt = new Option(optdefparams)
                    scheduledExecution.addToOptions(theopt)
                    EditOptsController._validateOption(theopt,null,scheduledExecution.scheduled)
                    if (theopt.errors.hasErrors() || !theopt.validate()) {
                        failed = true
                        theopt.discard()
                        def errmsg = optdefparams.name + ": " + theopt.errors.allErrors.collect {lookupMessageError(it)}.join(";")
                        scheduledExecution.errors.rejectValue(
                                'options',
                                'scheduledExecution.options.invalid.message',
                                [errmsg] as Object[],
                                'Invalid Option definition: {0}'
                        )
                    }
                    theopt.scheduledExecution = scheduledExecution
                    i++
                }
            }
        }
        
        parseOrchestratorFromParams(params)
        if(params.orchestrator){
            def result = _updateOrchestratorData(params, scheduledExecution)
            scheduledExecution.orchestrator.save()
            if (result.failed) {
                failed = result.failed
            }
        }else{
            scheduledExecution.orchestrator = null
        }
        
        parseNotificationsFromParams(params)
        if (params.notifications) {
            //create notifications
            def result = _updateNotificationsData(params, scheduledExecution,projectProps)
            if (result.failed) {
                failed = result.failed
            }
        }
        if (scheduledExecution.doNodedispatch) {
            if (!scheduledExecution.asFilter()) {
                scheduledExecution.errors.rejectValue('filter', 'scheduledExecution.filter.blank.message')
                failed = true
            } else if (!scheduledExecution.nodeThreadcount) {
                scheduledExecution.nodeThreadcount = 1
            }
        }
        failed = failed || !valid
        return [failed: failed, scheduledExecution: scheduledExecution]
    }

}

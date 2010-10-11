import java.util.Collections;

import javax.security.auth.Subject;

import org.quartz.Scheduler
import org.quartz.JobDetail
import org.quartz.Trigger
import org.quartz.CronTrigger
import org.quartz.TriggerUtils
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.InterruptableJob

import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.authentication.Group

/**
 *  ScheduledExecutionService manages scheduling jobs with the Quartz scheduler
 */
class ScheduledExecutionService {
    boolean transactional = false

    def FrameworkService frameworkService

    def Scheduler quartzScheduler
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
                    log.error "idlist value was not valid: ${it}"
                }
            }

        }
        if(!query.groupPath){
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
                        eq("id",theid)
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

        def total = ScheduledExecution.createCriteria().count{

            if(idlist){
                or{
                    idlist.each{ theid->
                        eq("id",theid)
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
            }else{
                or{
                    eq("groupPath","")
                    isNull("groupPath")
                }
            }
        };


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
    def getGroups(){
        def groups = ScheduledExecution.executeQuery("select distinct se.groupPath, count(*) from ScheduledExecution se where se.groupPath is not null group by se.groupPath" )
        def groupMap=[:]

        groups.each{
            def l = Arrays.asList(it)
            if(l[0]){
                groupMap[l[0]]=l[1]
            }
        }
        return groupMap
    }

    def rescheduleJobs(){
        def schedJobs=ScheduledExecution.findAllByScheduled(true)
        schedJobs.each{ ScheduledExecution se->
            try {
                scheduleJob(se,null,null)
                log.error("rescheduled job: ${se.id}")
            } catch (Exception e) {
                log.error("Job not rescheduled: ${se.id}: ${e.message}")
            }
        }
    }
    boolean convertNonWorkflow(ScheduledExecution se){
        def kprops=['argString','adhocLocalString','adhocRemoteString','adhocFilepath']
        def props = se.properties.findAll{it.key=~/^(type|name|command|argString|adhocExecution|adhoc.*String|adhocFilepath)$/}
        if(props){
            def Workflow workflow = new Workflow(threadcount:1,keepgoing:true,scheduledExecution:se)
            def cexec
            if(props.jobName){
                cexec = new JobExec(props)
            }else{
                cexec = new CommandExec(props)
            }
            if(!cexec.project){
                cexec.project=se.project
            }
            workflow.commands = new ArrayList()
            workflow.commands.add(cexec)
            se.workflow=workflow
            se.adhocExecution=false
            kprops.each{k->
                se[k]=null
            }
            return true
        }
        return false
    }
    def convertNonWorkflowJobs(){
        def nonwfjobs = ScheduledExecution.findAllByWorkflowIsNull()
        nonwfjobs.each{ScheduledExecution se->
            def ok=convertNonWorkflow(se)
            se.save()
            log.error("Converted non-workflow job: ${se.id}: success? ${ok}")
        }
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
    def Map getGroupTree(){
        def groupMap = getGroups()

        def groupTree=[:]
        groupMap.each{ String k,v ->
            def l = k.split("/")

            def parent=null
            def cmap=groupTree
            def i=0
            l.each{p ->
                def path=l[0..i].join("/")
                def subs
                if(!cmap[p]){
                    subs=[:]
                    cmap[p]=[count:groupMap[path]?groupMap[path]:0,subs:subs]
                }else{
                    subs=cmap[p]['subs']

                }
                cmap=subs
                i++
            }
        }
        return groupTree
    }

    def listNextScheduledJobs(int num){
        def list = ScheduledExecution.list(max: num, sort:'nextExecution')
        return list;
    }

    def deleteJob(String jobname, String groupname){
        log.info("deleting job from scheduler")
        quartzScheduler.deleteJob(jobname,groupname)
    }

    def userAuthorizedForJob(request,ScheduledExecution se, Framework framework){
        def resource = ["job": se.getJobName(), "group": se.getGroupPath() ?: ""]
        def environment = Collections.emptySet();
        def Decision d = framework.getAuthorizationMgr().evaluate(resource, request.subject,
            UserAuth.WF_READ, environment)
        return d.isAuthorized()
    }

    def scheduleJob(ScheduledExecution se, String oldJobName, String oldGroupName) {
        
        def jobDetail = createJobDetail(se)
        def trigger = createTrigger(se)
        def Date nextTime
        if(oldJobName && oldGroupName){
            def oldjob = quartzScheduler.getJobDetail(oldJobName,oldGroupName)
            log.info("job renamed, removing old job and scheduling new one")
            quartzScheduler.deleteJob(oldJobName,oldGroupName)
        }
        if ( hasJobScheduled(se) ) {
            log.info("rescheduling existing job: " + se.generateJobScheduledName())
            
            nextTime = quartzScheduler.rescheduleJob(se.generateJobScheduledName(), se.generateJobGroupName(), trigger)
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
     * Schedule a stored job to execute immediately.
     */
    def long scheduleTempJob(ScheduledExecution se, String user, List roleList, Execution e) {

        def jobDetail = createJobDetail(se, "TEMP:"+user +":"+se.id+":"+e.id, user+":run:"+se.id)
        jobDetail.getJobDataMap().put("userRoles",roleList.join(","))
        jobDetail.getJobDataMap().put("executionId",e.id.toString())

        def Trigger trigger = TriggerUtils.makeImmediateTrigger(0,0)
        trigger.setName(jobDetail.getName()+"Trigger")
        def nextTime
        try {
            log.info("scheduling immediate job run: " + jobDetail.getName())
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        } catch (Exception exc) {
            throw new RuntimeException("caught exception while adding job: " +exc.getMessage(), exc)
        }
        return e.id
    }
    /**
     * Schedule a temp job to execute immediately.
     */
    def long scheduleTempJob(String user, List roleList, Map params, Execution e) {
        def ident=getJobIdent(null,e);
        def jobDetail = new JobDetail(ident.jobname, ident.groupname, ExecutionJob)
        jobDetail.setDescription("Execute command: "+e)
        jobDetail.getJobDataMap().put("isTempExecution","true")
        jobDetail.getJobDataMap().put("executionId",e.id.toString())
        jobDetail.getJobDataMap().put("rdeck.base",frameworkService.getRundeckBase())
        jobDetail.getJobDataMap().put("userRoles",roleList.join(","))
//        jobDetail.addJobListener("sessionBinderListener")
        jobDetail.addJobListener("defaultGrailsServiceInjectorJobListener")

        def Trigger trigger = TriggerUtils.makeImmediateTrigger(0,0)
        trigger.setName(jobDetail.getName()+"Trigger")
        def nextTime
        try {
            log.info("scheduling temp job: " + jobDetail.getName())
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        } catch (Exception exc) {
            throw new RuntimeException("caught exception while adding job: " +exc.getMessage(), exc)
        }
        return e.id
    }

    def JobDetail createJobDetail(ScheduledExecution se) {
        return createJobDetail(se,se.generateJobScheduledName(), se.generateJobGroupName())
    }
    def JobDetail createJobDetail(ScheduledExecution se, String jobname, String jobgroup){
        def jobDetail = new JobDetail(jobname,jobgroup, ExecutionJob)
        jobDetail.setDescription(se.description)
        jobDetail.getJobDataMap().put("scheduledExecutionId",se.id.toString())
        jobDetail.getJobDataMap().put("rdeck.base",frameworkService.getRundeckBase())
//            jobDetail.addJobListener("sessionBinderListener")
        jobDetail.addJobListener("defaultGrailsServiceInjectorJobListener")
        return jobDetail
    }

    def Trigger createTrigger(ScheduledExecution se) {
        def CronTrigger trigger
        def cronExpression = se.generateCrontabExression()
        try {
            log.info("creating trigger with crontab expression: " + cronExpression)
            trigger = new CronTrigger(se.generateJobScheduledName(), se.generateJobGroupName(),
                                      se.generateJobScheduledName(), se.generateJobGroupName(),
                                      cronExpression)
        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression )
        }
        return trigger
    }

    def boolean hasJobScheduled(ScheduledExecution se) {
        def boolean scheduled = false
        def names = Arrays.asList(quartzScheduler.getJobNames(se.generateJobGroupName()))
        return names.contains(se.generateJobScheduledName())
    }

    def Map nextExecutionTimes(Collection scheduledExecutions) {
        def map = [ : ]
        log.info("looking up getNextFireTime for ["+scheduledExecutions.size()+ "] objects")
        scheduledExecutions.each {
            def next = nextExecutionTime(it)
            if(next){
                map[it.id] = next
            }
        }
        return map
    }

    public static final long TWO_HUNDRED_YEARS=1000l * 60l * 60l * 24l * 365l * 200l
    def Date nextExecutionTime(ScheduledExecution se) {
        if(!se.scheduled){
            return new Date(TWO_HUNDRED_YEARS)
        }
        def trigger = quartzScheduler.getTrigger(se.generateJobScheduledName(), se.generateJobGroupName())
        if(trigger){
            return trigger.getNextFireTime()
        }else{
            return null;
        }
    }

    def Date tempNextExecutionTime(ScheduledExecution se){
        def trigger = createTrigger(se)
        return trigger.getNextFireTime()
    }
}

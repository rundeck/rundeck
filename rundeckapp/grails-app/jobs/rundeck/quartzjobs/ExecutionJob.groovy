package rundeck.quartzjobs

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import org.quartz.JobExecutionContext
import com.dtolabs.rundeck.core.common.Framework
import org.quartz.InterruptableJob

import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ExecutionService
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService

class ExecutionJob implements InterruptableJob {

    public static final int DEFAULT_STATS_RETRY_MAX = 3
    public static final long DEFAULT_STATS_RETRY_DELAY = 5000
    public static final int DEFAULT_FINALIZE_RETRY_MAX = 10
    public static final long DEFAULT_FINALIZE_RETRY_DELAY = 5000

    /**
     * max retry count for updating Job stats when execution completes
     */
    int statsRetryMax = DEFAULT_STATS_RETRY_MAX

    /**
     * milliscond delay between retries to update Job stats
     */
    long statsRetryDelay = DEFAULT_STATS_RETRY_DELAY

    /**
     * max retry count for finalizing Execution state when complete
     */
    int finalizeRetryMax = DEFAULT_FINALIZE_RETRY_MAX

    /**
     * millisecond delay between retries to finalize execution state
     */
    long finalizeRetryDelay = DEFAULT_FINALIZE_RETRY_DELAY
    def boolean wasInterrupted
    def boolean wasTimeout
    def grailsApplication
    static triggers = {
        /** define no triggers here */
    }
    // Implements the Job interface, execute
    void execute(JobExecutionContext context) {
        grailsApplication= context.jobDetail.jobDataMap.get('grailsApplication')
        if(grailsApplication?.config?.rundeck?.execution?.finalize?.retryMax){
            finalizeRetryMax= asInt(grailsApplication.config.rundeck?.execution?.finalize?.retryMax)
        }
        if(grailsApplication?.config?.rundeck?.execution?.finalize?.retryDelay){
            finalizeRetryDelay= asInt(grailsApplication.config.rundeck?.execution?.finalize?.retryDelay)
        }
        if(grailsApplication?.config?.rundeck?.execution?.stats?.retryMax){
            statsRetryMax= asInt(grailsApplication.config.rundeck?.execution?.stats?.retryMax)
        }
        if(grailsApplication?.config?.rundeck?.execution?.stats?.retryDelay){
            statsRetryDelay= asInt(grailsApplication.config.rundeck?.execution?.stats?.retryDelay)
        }
        MetricRegistry metricRegistry=context.jobDetail.jobDataMap.get('metricRegistry')
        if(metricRegistry){
            Timer executionTimer=metricRegistry.timer(MetricRegistry.name(ExecutionJob, 'executionTimer'))
            executionTimer.time {
                execute_internal(context)
            }
        }else{
            execute_internal(context)
        }
    }

    private int asInt(def value) {
        if(value instanceof String){
            return value.toInteger()
        }else if(value instanceof Integer){
            return value
        }else{
            throw new IllegalArgumentException("Not able to convert to integer, value: ${value}")
        }
    }

    void execute_internal(JobExecutionContext context) {
        def boolean success=false
        def Map initMap
        try{
            initMap= initialize(context,context.jobDetail.jobDataMap)
        }catch(Throwable t){
            log.error("Unable to start Job execution: ${t.message?t.message:'no message'}",t)
            return
        }
        if(initMap.jobShouldNotRun){
            log.info(initMap.jobShouldNotRun)
            return
        }
        def result
        try {
            if(!wasInterrupted){
                result=executeCommand(initMap.executionService, initMap.executionUtilService,initMap.execution,
                        initMap.framework,initMap.authContext,initMap.scheduledExecution, initMap.timeout?:0,
                        initMap.extraParams, initMap.extraParamsExposed)
                success=result.success
            }
        }catch(Throwable t){
            log.error("Failed execution ${initMap.execution.id} : ${t.message?t.message:'no message'}",t)
        }
        saveState(initMap.executionService, initMap.execution ? initMap.execution : (Execution) null, success,
            wasInterrupted, wasTimeout, initMap.isTemp,
                initMap.scheduledExecutionId ? initMap.scheduledExecutionId: -1L,
                result?.execmap)
    }

    /**
     * Attempt to get the list of failed nodes from a caught NodesetFailureException if the thread is
     * @return the list of failed node names, or null
     */
    private static Map<String,Object> extractFailedNodes(Map execmap=null) {
        if(null==execmap){
            return null;
        }
        if(null!=execmap.noderecorder && execmap.noderecorder instanceof NodeRecorder){
            final recorder = (NodeRecorder) execmap.noderecorder
            return recorder.getFailedNodes()
        }
        return null;
    }
    /**
     * Attempt to get the list of failed nodes from a caught NodesetFailureException if the thread is
     * @return the list of failed node names, or null
     */
    private static Set<String> extractSucceededNodes(Map execmap=null) {
        if(null==execmap){
            return null;
        }
        if(null!=execmap.noderecorder && execmap.noderecorder instanceof NodeRecorder){
            final recorder = (NodeRecorder) execmap.noderecorder
            def nodes = recorder.getSuccessfulNodes()
            return nodes
        }
        return null;
    }

    public void interrupt(){
        wasInterrupted=true;
    }

    def initialize(JobExecutionContext context, def jobDataMap) {
        def initMap=[:]
//        def jobDetail = context.getJobDetail()
//        def jobDataMap = jobDetail.getJobDataMap()
        initMap.isTemp = "true"==jobDataMap.get("isTempExecution")
        if(initMap.isTemp){
            //temp execution, means no associated ScheduledExecution object
            initMap.executionId=jobDataMap.get("executionId")
            if(!initMap.executionId){
                throw new RuntimeException("executionId was not found in job data map for temporary execution")
            }
        }else{
            initMap.scheduledExecution = fetchScheduledExecution(jobDataMap)
            initMap.scheduledExecution.refresh()
            if(!initMap.scheduledExecution){
                throw new RuntimeException("scheduledExecution data was not found in job data map")
            }
            initMap.scheduledExecutionId=initMap.scheduledExecution.id
        }
        initMap.timeout = jobDataMap.get('timeout')
        initMap.executionService = fetchExecutionService(jobDataMap)
        initMap.executionUtilService = fetchExecutionUtilService(jobDataMap)
        initMap.frameworkService = fetchFrameworkService(jobDataMap)
        initMap.adbase = jobDataMap.get("rdeck.base")
        if(initMap.isTemp){
            initMap.execution = Execution.get(initMap.executionId)
            if (!initMap.execution) {
                throw new RuntimeException("failed to lookup Exception object from job data map: id: ${initMap.executionId}")
            }
            if (! initMap.execution instanceof Execution) {
                throw new RuntimeException("JobDataMap contained invalid Execution type: " + initMap.execution.getClass().getName())
            }
            initMap.execution.refresh()
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            def subject = jobDataMap.get("userSubject")
            if(subject){
                initMap.authContext = frameworkService.getAuthContextForSubject(subject)
            }else{
                def roles = jobDataMap.get("userRoles")
                if (null == roles) {
                    throw new RuntimeException("userRoleList not found in job data map")
                }
                def rolelist = Arrays.asList(roles.split(","))
                initMap.authContext = frameworkService.getAuthContextForUserAndRoles(initMap.execution.user, rolelist)
            }
        }else if(jobDataMap.get("executionId")){
                initMap.executionId=jobDataMap.get("executionId")
                initMap.extraParams=jobDataMap.get("extraParams")
                initMap.extraParamsExposed=jobDataMap.get("extraParamsExposed")
                initMap.execution = Execution.get(initMap.executionId)
                //NOTE: Oracle/hibernate bug workaround: if session has not flushed we may have to wait until Execution.get
                //can return the right entity
                int retry=30
                if(!initMap.execution){
                    log.warn("ExecutionJob: Execution not found with ID [${initMap.executionId}], will retry for up to 60 seconds...")
                }
                while(!initMap.execution && retry>0){
                    Thread.sleep(2000)
                    initMap.execution = Execution.get(initMap.executionId)
                    retry--;
                }
                if (!initMap.execution) {
                    throw new RuntimeException("Failed to find Execution with id: ${initMap.executionId}")
                }
                if(retry<30){
                    log.info("ExecutionJob: Execution found with ID [${initMap.executionId}] retried (${30-retry})")
                }
                if (! initMap.execution instanceof Execution) {
                    throw new RuntimeException("JobDataMap contained invalid Execution type: " + initMap.execution.getClass().getName())
                }
                initMap.execution.refresh()
                FrameworkService frameworkService = initMap.frameworkService
                initMap.framework = frameworkService.rundeckFramework
                def subject = jobDataMap.get("userSubject")
                if (subject) {
                    initMap.authContext = frameworkService.getAuthContextForSubject(subject)
                } else {
                    def roles = jobDataMap.get("userRoles")
                    if (null == roles) {
                        throw new RuntimeException("userRoleList not found in job data map")
                    }
                    def rolelist = Arrays.asList(roles.split(","))
                    initMap.authContext = frameworkService.getAuthContextForUserAndRoles(initMap.execution.user, rolelist)
                }
        }else{
            def serverUUID = jobDataMap.get("serverUUID")
            if (serverUUID != null && jobDataMap.get("bySchedule")) {
                //verify scheduled job should be run on this node in cluster mode
                if (serverUUID!=initMap.scheduledExecution.serverNodeUUID){
                    initMap.jobShouldNotRun="Job ${initMap.scheduledExecution.extid} will run on server ID ${initMap.scheduledExecution.serverNodeUUID}, removing schedule on this node."
                    context.getScheduler().deleteJob(context.jobDetail.name,context.jobDetail.group)
                    return initMap
                }
            }
//            initMap.framework = FrameworkService.getFrameworkForUserAndRoles(initMap.scheduledExecution.user,initMap.scheduledExecution.userRoles,initMap.adbase)
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            def rolelist = initMap.scheduledExecution.userRoles
            initMap.authContext = frameworkService.getAuthContextForUserAndRoles(initMap.scheduledExecution.user, rolelist)
            initMap.extraParamsExposed = initMap.executionService.selectSecureOptionInput(initMap.scheduledExecution,[:],true)
            initMap.execution = initMap.executionService.createExecution(initMap.scheduledExecution, initMap.framework,initMap.scheduledExecution.user)
        }
        return initMap
    }

    def executeCommand(ExecutionService executionService, ExecutionUtilService executionUtilService,
                       Execution execution, Framework framework, AuthContext authContext,
                       ScheduledExecution scheduledExecution = null, long timeout, Map extraParams = null,
                       Map extraParamsExposed = null) {

        def success = false
        def Map execmap
        try {
            execmap = executionService.executeAsyncBegin(framework, authContext, execution, scheduledExecution,
                    extraParams, extraParamsExposed)

        } catch (Exception e) {
            log.error("Execution ${execution.id} failed to start: " + e.getMessage(), e)
            throw e
        }
        if (!execmap) {
            //failed to start
            return [success: false]
        }
        def timeoutms=1000*timeout
        def shouldCheckTimeout = timeoutms > 0
        long startTime=System.currentTimeMillis()
        int killcount = 0;
        def killLimit = 100
        def ServiceThreadBase thread = execmap.thread
        while (thread.isAlive()) {
            try {
                thread.join(1000)
            } catch (InterruptedException e) {
                //do nada
            }
            if (!wasInterrupted && !wasTimeout
                    && shouldCheckTimeout
                    && (System.currentTimeMillis() - startTime) > timeoutms) {
                wasTimeout=true
                interrupt()
            }
            if (wasInterrupted) {
                if (killcount < killLimit) {
                    //send wave after wave
                    thread.abort()
                    Thread.yield();
                    killcount++;
                } else {
                    //reached pre-set kill limit, so shut down
                    thread.stop()
                }
            }
        }


        def boolean retrysuccess
        def Throwable exc
        (retrysuccess, exc) = withRetry(finalizeRetryMax, finalizeRetryDelay,
                "Execution ${execution.id} finishExecution:") {
            executionUtilService.finishExecution(execmap)
        }
        if (!retrysuccess && exc) {
            throw new RuntimeException("Execution ${execution.id} failed: " + exc.getMessage(), exc)
        }

        log.debug("ExecutionJob: execution successful? " + success + ", interrupted? " + wasInterrupted+", " +
                "timeout? "+wasTimeout)

        return [success: thread.isSuccessful(), execmap: execmap]

    }
    /**
     * Execute a closure and if a throwable is thrown, retry a specified number of times with intermediate thread sleep
     * @param max maximum times to retry, or -1 for no maximum
     * @param sleep millisecond sleep between retries
     * @param identity string identifying the action
     * @param action action to retry
     * @return true if execution of action was accomplished without exception
     */
    def List withRetry(int max, long sleep, String identity, Closure action){
        int count=0
        boolean complete=false
        Throwable caught=null
        while(!complete && (max>count || max<0)){
            if(count>0){
                log.warn(identity + " failed (attempts=${count}/${max}), retrying in " + sleep + "ms")
                try {
                    Thread.sleep(sleep)
                } catch (InterruptedException e) {
                    log.error(identity + " retry was interrupted, failing")
                    break
                }
                if(wasInterrupted){
                    log.error(identity + " retry was interrupted, failing")
                    break
                }
            }
            count++
            try{
                action.call()
                complete=true
                caught=null
            }catch (Throwable t){
                caught=t
                log.error(identity + " caught exception: ${caught.message}", caught)
            }
        }
        if(!complete && caught){
            log.error(identity + " failed (attempts=${count}/${max}) with exception: ${caught.message}")
        }else if(complete && count>1){
            log.warn(identity + " completed after (attempts=${count}/${max})")
        }
        return [complete,caught]
    }

    def saveState(ExecutionService executionService,Execution execution, boolean success, boolean _interrupted,
            boolean timedOut,
                  boolean isTemp, long scheduledExecutionId=-1, Map execmap) {
        Map<String,Object> failedNodes=extractFailedNodes(execmap)
        Set<String> succeededNodes=extractSucceededNodes(execmap)

        //save Execution state
        def dateCompleted = new Date()
        def resultMap = [
                status: String.valueOf(success),
                dateCompleted: dateCompleted,
                cancelled: _interrupted && !timedOut,
                timedOut: timedOut,
                failedNodes: failedNodes?.keySet(),
                failedNodesMap: failedNodes,
                succeededNodes: succeededNodes,
        ]
        def saveStateComplete=false
        def saveStateException=null
        //attempt to save execution state, with retry, in case DB connection fails
        (saveStateComplete, saveStateException) = withRetry(finalizeRetryMax, finalizeRetryDelay,
                "Execution ${execution.id} save result status:") {
            executionService.saveExecutionState(scheduledExecutionId > 0 ? scheduledExecutionId : null, execution.id, resultMap, execmap)
        }
        if (!saveStateComplete) {
            log.error("ExecutionJob: Failed to save execution state for ${execution.id}, after retrying ${finalizeRetryMax} times: ${saveStateException}")
        }
        if(!isTemp && scheduledExecutionId && success) {
            //update ScheduledExecution statistics for successful execution
            def time = dateCompleted.time - execution.dateStarted.time
            def savedJobState = false
            withRetry(statsRetryMax, statsRetryDelay, "Execution ${execution.id} update job stats (${scheduledExecutionId}):") {
                savedJobState = executionService.updateScheduledExecStatistics(scheduledExecutionId, execution.id, time)
            }
            if (!savedJobState) {
                log.error("ExecutionJob: Failed to update job statistics for ${execution.id}, after retrying ${statsRetryMax} times")
            }

        }
        return saveStateComplete
    }

    def ScheduledExecution fetchScheduledExecution(def jobDataMap) {
        def seid = jobDataMap.get("scheduledExecutionId")
        def ScheduledExecution se = ScheduledExecution.get(seid)
        if (!se) {
            throw new RuntimeException("failed to lookup scheduledException object from job data map: id: ${seid}")
        }
        if (! se instanceof ScheduledExecution) {
            throw new RuntimeException("JobDataMap contained invalid ScheduledExecution type: " + se.getClass().getName())
        }
        return se
    }

    def ExecutionService fetchExecutionService(def jobDataMap) {
        def es = jobDataMap.get("executionService")
        if (es==null) {
            throw new RuntimeException("ExecutionService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof ExecutionService)) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionService type: " + es.getClass().getName())
        }
        return es

    }
    def ExecutionUtilService fetchExecutionUtilService(def jobDataMap) {
        def es = jobDataMap.get("executionUtilService")
        if (es==null) {
            throw new RuntimeException("ExecutionUtilService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof ExecutionUtilService)) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionUtilService type: " + es.getClass().getName())
        }
        return es
    }
    def FrameworkService fetchFrameworkService(def jobDataMap) {
        def es = jobDataMap.get("frameworkService")
        if (es==null) {
            throw new RuntimeException("FrameworkService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof FrameworkService)) {
            throw new RuntimeException("JobDataMap contained invalid FrameworkService type: " + es.getClass().getName())
        }
        return es
    }
}

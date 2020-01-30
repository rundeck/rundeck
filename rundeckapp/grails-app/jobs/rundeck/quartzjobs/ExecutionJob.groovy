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

package rundeck.quartzjobs

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.quartz.JobExecutionContext
import com.dtolabs.rundeck.core.common.Framework
import org.quartz.InterruptableJob
import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import org.rundeck.util.Sizes
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.ExecutionUtilService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulesService
import rundeck.services.execution.ThresholdValue
import rundeck.services.logging.LoggingThreshold

import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ExecutionJob implements InterruptableJob {

    public static final int DEFAULT_STATS_RETRY_MAX = 5
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
    def boolean wasThreshold
    def boolean wasTimeout
    GrailsApplication grailsApplication
    Long executionId
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
        } catch (ExecutionServiceException es) {
            if (es.code == 'conflict') {
                log.error("Unable to start Job execution: ${es.message ?: 'no message'}")
                return
            } else {
                log.error("Unable to start Job execution: ${es.message ?: 'no message'}", es)
                throw es
            }
        }catch(Throwable t){
            log.error("Unable to start Job execution: ${t.message?t.message:'no message'}",t)
            throw t
        }
        executionId = initMap.executionId ? Long.parseLong(initMap.executionId) : initMap.execution?.id
        if(initMap.jobShouldNotRun){
            log.info(initMap.jobShouldNotRun)
            return
        }
        def result
        def statusString=null
        try {
            if(!wasInterrupted){
                ExecutionService executionService = initMap.executionService
                ExecutionUtilService service = initMap.executionUtilService
                Execution execution = initMap.execution
                if(context.trigger.jobDataMap.get('scheduleArgs')){
                    execution.argString = context.trigger.jobDataMap.get('scheduleArgs')
                    execution.save(flush: true)
                }
                Framework framework = initMap.framework
                UserAndRolesAuthContext context1 = initMap.authContext
                ScheduledExecution job = initMap.scheduledExecution
                def timeout = initMap.timeout
                Map secureOpts = initMap.secureOpts
                Map secureOptsExposed = initMap.secureOptsExposed
                int retryAttempt = context.jobDetail.jobDataMap.get("retryAttempt")?:0
                result = executeCommand(
                        executionService,
                        service,
                        execution,
                        framework,
                        context1,
                        job,
                        timeout ?: 0,
                        secureOpts,
                        secureOptsExposed,
                        retryAttempt
                )

                success=result.success
                statusString=Execution.isCustomStatusString(result.result?.statusString)?result.result?.statusString:null
            }
        }catch(Throwable t){
            log.error("Failed execution ${initMap.execution.id} : ${t.message?t.message:'no message'}",t)
        }
        saveState(
                context.jobDetail.jobDataMap,
                initMap.executionService,
                initMap.execution ? initMap.execution : (Execution) null,
                success,
                wasInterrupted,
                wasTimeout,
                initMap.isTemp,
                statusString,
                initMap.scheduledExecutionId ? initMap.scheduledExecutionId : -1L,
                initMap,
                result?.execmap
        )
    }

    /**
     * Attempt to get the list of failed nodes
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
     * Attempt to get the list of successful nodes
     * @return the list of successful node names, or null
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
        def initMap = [:]
        initMap.isTemp = "true" == jobDataMap.get("isTempExecution")
        if (initMap.isTemp) {
            //temp execution, means no associated ScheduledExecution object
            initMap.executionId = jobDataMap.get("executionId")
            if (!initMap.executionId) {
                throw new RuntimeException("executionId was not found in job data map for temporary execution")
            }
        } else {
            initMap.scheduledExecution = fetchScheduledExecution(jobDataMap)
            if (!initMap.scheduledExecution) {
                throw new RuntimeException("scheduledExecution data was not found in job data map")
            }
            initMap.scheduledExecutionId = initMap.scheduledExecution.id
        }

        initMap.executionService = fetchExecutionService(jobDataMap)
        initMap.executionUtilService = fetchExecutionUtilService(jobDataMap)
        initMap.frameworkService = fetchFrameworkService(jobDataMap)
        initMap.jobSchedulesService = fetchJobSchedulesService(jobDataMap)
        if (initMap.scheduledExecution?.timeout){
            initMap.timeout = initMap.scheduledExecution.timeoutDuration
        }

        if(initMap.isTemp){
            //an adhoc execution without associated job
            initMap.execution = fetchExecution(initMap.executionId)
            if (!initMap.execution) {
                throw new RuntimeException("failed to lookup Exception object from job data map: id: ${initMap.executionId}")
            }
            if (! initMap.execution instanceof Execution) {
                throw new RuntimeException("JobDataMap contained invalid Execution type: " + initMap.execution.getClass().getName())
            }
            initMap.execution.refresh()
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            initMap.authContext=jobDataMap.get('authContext')
        }else if(jobDataMap.get("executionId")){
            //a job execution invoked by a user
            initMap.executionId=jobDataMap.get("executionId")
            initMap.secureOpts=jobDataMap.get("secureOpts")
            initMap.secureOptsExposed=jobDataMap.get("secureOptsExposed")
            initMap.execution = fetchExecution(initMap.executionId)
            //NOTE: Oracle/hibernate bug workaround: if session has not flushed we may have to wait until Execution.get
            //can return the right entity
            int retry=30
            if(!initMap.execution){
                log.warn("ExecutionJob: Execution not found with ID [${initMap.executionId}], will retry for up to 60 seconds...")
            }
            while(!initMap.execution && retry>0){
                Thread.sleep(2000)
                initMap.execution = fetchExecution(initMap.executionId)
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
            def jobArguments=initMap.frameworkService.parseOptsFromString(initMap.execution?.argString)
            if (initMap.scheduledExecution?.timeout && initMap.scheduledExecution?.timeout.contains('${')) {
                def timeout = DataContextUtils.replaceDataReferencesInString(initMap.scheduledExecution?.timeout,
                        DataContextUtils.addContext("option", jobArguments, null))
                initMap.timeout = timeout ? Sizes.parseTimeDuration(timeout) : -1
            }
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            initMap.authContext = jobDataMap.get('authContext')
        }else{
            //a scheduled job that was triggered
            def serverUUID = jobDataMap.get("serverUUID")
            if (serverUUID != null && jobDataMap.get("bySchedule")) {
                //verify scheduled job should be run on this node in cluster mode
                if (serverUUID!=initMap.scheduledExecution.serverNodeUUID){
                    if(!initMap.jobSchedulesService.isScheduled(initMap.scheduledExecution.uuid)){
                        initMap.jobShouldNotRun="Job ${initMap.scheduledExecution.extid} schedule has been stopped by ${initMap.scheduledExecution.serverNodeUUID}, removing schedule on this server (${serverUUID})."
                    }else if(!initMap.scheduledExecution.shouldScheduleExecution()){
                        initMap.jobShouldNotRun="Job ${initMap.scheduledExecution.extid} schedule/execution has been disabled by ${initMap.scheduledExecution.serverNodeUUID}, removing schedule on this server (${serverUUID})."
                    }else{
                        initMap.jobShouldNotRun="Job ${initMap.scheduledExecution.extid} will run on server ID ${initMap.scheduledExecution.serverNodeUUID}, removing schedule on this server (${serverUUID})."
                    }
                    context.getScheduler().deleteJob(context.jobDetail.key)
                    return initMap
                }else{
                    //verify run on this node but scheduled disabled
                    if(!initMap.jobSchedulesService.shouldScheduleExecution(initMap.scheduledExecution.uuid)){
                        initMap.jobShouldNotRun = "Job ${initMap.scheduledExecution.extid} schedule has been disabled, removing schedule on this server (${serverUUID})."
                        context.getScheduler().deleteJob(context.jobDetail.key)
                        return initMap
                    }
                }
            }

            FrameworkService frameworkService = initMap.frameworkService
            def project = initMap.scheduledExecution.project
            def fwProject = frameworkService.getFrameworkProject(project)
            def disableEx = fwProject.getProjectProperties().get("project.disable.executions")
            def disableSe = fwProject.getProjectProperties().get("project.disable.schedule")
            def isProjectExecutionEnabled = ((!disableEx)||disableEx.toLowerCase()!='true')
            def isProjectScheduledEnabled = ((!disableSe)||disableSe.toLowerCase()!='true')

            if(!(isProjectExecutionEnabled && isProjectScheduledEnabled)){
                initMap.jobShouldNotRun = "Job ${initMap.scheduledExecution.extid} schedule has been disabled, removing schedule on this project (${initMap.scheduledExecution.project})."
                context.getScheduler().deleteJob(context.jobDetail.key)
                return initMap
            }


            initMap.framework = frameworkService.rundeckFramework
            def rolelist = initMap.scheduledExecution.userRoles
            initMap.authContext = frameworkService.getAuthContextForUserAndRolesAndProject(
                    initMap.scheduledExecution.user,
                    rolelist,
                    project
            )
            initMap.secureOptsExposed = initMap.executionService.selectSecureOptionInput(initMap.scheduledExecution,[:],true)
            initMap.execution = initMap.executionService.createExecution(initMap.scheduledExecution,initMap.authContext,null,[executionType:'scheduled'])
        }
        if (!initMap.authContext) {
            throw new RuntimeException("authContext could not be determined")
        }
        return initMap
    }

    def executeCommand(
            ExecutionService executionService,
            ExecutionUtilService executionUtilService,
            Execution execution,
            Framework framework,
            UserAndRolesAuthContext authContext,
            ScheduledExecution scheduledExecution,
            long timeout,
            Map secureOpts,
            Map secureOptsExposed,
            int retryAttempt = 0
    )
    {

        def success = true
        def Map execmap
        try {
            execmap = executionService.executeAsyncBegin(
                    framework,
                    authContext,
                    execution,
                    scheduledExecution,
                    secureOpts,
                    secureOptsExposed
            )

        } catch (Exception e) {
            log.error("Execution ${execution.id} failed to start: " + e.getMessage(), e)
            throw e
        }
        if (!execmap) {
            //failed to start
            return [success: false]
        }
        def timeoutms = 1000 * timeout
        def shouldCheckTimeout = timeoutms > 0
        long startTime = System.currentTimeMillis()
        int killcount = 0;
        def killLimit = 100
        def WorkflowExecutionServiceThread thread = execmap.thread
        def Consumer<Long> periodicCheck = execmap.periodicCheck
        def ThresholdValue threshold = execmap.threshold
        def jobAverageDuration
        ScheduledExecution.withTransaction {
            jobAverageDuration = execmap.scheduledExecution?execmap.scheduledExecution.averageDuration:0
        }

        def boolean avgNotificationSent = false
        def boolean stop=false


        def jobAverageDurationFinal = getNotifyAvgDurationThreshold(execmap.scheduledExecution?execmap.scheduledExecution.notifyAvgDurationThreshold:"0",
                                                                    jobAverageDuration,
                                                                    thread?.context?.dataContext
                                    )

        def context = execmap?.thread?.context
        boolean never=true
        while (thread.isAlive() || never) {
            never=false
            try {
                thread.join(1000)
            } catch (InterruptedException e) {
                //do nada
            }
            def duration = System.currentTimeMillis() - startTime
            if(!avgNotificationSent && jobAverageDurationFinal>0){
                if(duration > jobAverageDurationFinal){
                    def res = executionService.avgDurationExceeded(
                            execmap.scheduledExecution.id,
                            [
                                    execution: execmap.execution,
                                    context:context
                            ]
                    )
                    avgNotificationSent=true
                }
            }
            periodicCheck?.accept(duration)
            if (
            !wasInterrupted
                    && !wasTimeout
                    && shouldCheckTimeout
                    && duration > timeoutms
            ) {
                wasTimeout = true
                interrupt()
                success=false
            }else if(threshold && threshold.isThresholdExceeded()){
                if(threshold.action == LoggingThreshold.ACTION_HALT) {
                    wasThreshold = true
                    success = false
                    stop = true
                }
            }
            if (wasInterrupted || stop) {
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
        (retrysuccess, exc) = withRetry(
            finalizeRetryMax,
            finalizeRetryDelay,
            "Execution ${execution.id} finishExecution:",
            executionService.&isApplicationShutdown
        ) {
            executionUtilService.finishExecution(execmap)
            true
        }
        if (!retrysuccess && exc) {
            throw new RuntimeException("Execution ${execution.id} failed: " + exc.getMessage(), exc)
        }

        log.debug(
                "ExecutionJob: execution successful? " + (success && thread.isSuccessful()) +
                        ", interrupted? " +
                        wasInterrupted +
                        ", " +
                        "timeout? " +
                        wasTimeout
                        +" threshold? "+threshold
        )

        return [success: success && thread.isSuccessful(), execmap: execmap, result: thread.result]

    }
    /**
     * Execute a closure and if a throwable is thrown, retry a specified number of times with intermediate thread sleep
     * @param max maximum times to retry, or -1 for no maximum
     * @param sleep millisecond sleep between retries
     * @param identity string identifying the action
     * @param shortcircuit optional closure called each time, if it returns true the retry loop is halted
     * @param action action to retry
     * @return true if execution of action was accomplished without exception
     */
    def List withRetry(int max, long sleep, String identity, Closure shortcircuit = null, Closure action) {
        int count=0
        boolean complete=false
        def backoff=1.5
        def jitter ={
            Math.floor(Math.random()*(sleep))
        }
        long newsleep=sleep+jitter()
        Throwable caught=null
        def isshortcircuit = shortcircuit?.call()
        while (!complete && (max > count || max < 0) && !(isshortcircuit)) {
            if(count>0){
                log.warn(identity + " failed (attempts=${count}/${max}), retrying in " + newsleep + "ms")
                try {
                    Thread.sleep(newsleep)
                } catch (InterruptedException e) {
                    log.error(identity + " retry was interrupted, failing")
                    break
                }
                if(wasInterrupted){
                    log.error(identity + " retry was interrupted, failing")
                    break
                }
                newsleep = Math.floor(newsleep*backoff)
            }
            count++
            try{
                def result=action.call()
                complete=result?true:false
                caught=null
            }catch (Throwable t){
                caught=t
                log.error(identity + " caught exception: ${caught.message}", caught)
            }
            isshortcircuit = shortcircuit?.call()
        }
        if(!complete && caught){
            log.error(identity + " failed (attempts=${count}/${max}) with exception: ${caught.message}")
        }else if(complete && count>1){
            log.warn(identity + " completed after (attempts=${count}/${max})")
        } else if (!complete && isshortcircuit) {
            caught = new Exception("retry halted due to application shutdown")
        }
        return [complete,caught]
    }

    def saveState(
            def jobDataMap,
            ExecutionService executionService,
            Execution execution,
            boolean success,
            boolean _interrupted,
            boolean timedOut,
            boolean isTemp,
            String statusString,
            long scheduledExecutionId = -1,
            Map initMap,
            Map execmap
    )
    {
        Map<String, Object> failedNodes = extractFailedNodes(execmap)
        Set<String> succeededNodes = extractSucceededNodes(execmap)

        if(wasThreshold && execmap.threshold?.action==LoggingThreshold.ACTION_HALT){
            //use custom status or fail
            success=false
            statusString = initMap.scheduledExecution?.logOutputThresholdStatus?:'failed'
        }
        //save Execution state
        def dateCompleted = new Date()
        def resultMap = [
                status        : statusString?: success? ExecutionState.succeeded.toString():ExecutionState.failed.toString(),
                dateCompleted : dateCompleted,
                cancelled     : _interrupted && !timedOut && !wasThreshold,
                timedOut      : timedOut,
                failedNodes   : failedNodes?.keySet(),
                failedNodesMap: failedNodes,
                succeededNodes: succeededNodes,
        ]
        def saveStateComplete = false
        def saveStateException = null
        Map retryContext = [
                user             : execution.user,
                authContext      : jobDataMap?.get("authContext") ?: initMap?.get("authContext"),
                secureOpts       : jobDataMap?.get("secureOpts"),
                secureOptsExposed: jobDataMap?.get("secureOptsExposed"),
                retryAttempt     : jobDataMap?.get("retryAttempt"),
                timeout          : jobDataMap?.get("timeout"),
        ]
        Closure action={
            executionService.saveExecutionState(
                    scheduledExecutionId > 0 ? scheduledExecutionId : null,
                    execution.id,
                    resultMap,
                    execmap,
                    retryContext
            )
            true
        }
        //attempt to save execution state, with retry, in case DB connection fails
        if(finalizeRetryMax>1) {
            (saveStateComplete, saveStateException) = withRetry(finalizeRetryMax, finalizeRetryDelay,
                                                                "Execution ${execution.id} save result status:",
                                                                executionService.&isApplicationShutdown,
                                                                action
            )
            if (!saveStateComplete) {
                execution.refresh()
                log.error("ExecutionJob: Failed to save execution state for ${execution.id}, after retrying ${finalizeRetryMax} times: ${saveStateException}")
            }
        }else{
            action.call()
        }
        if (!isTemp && scheduledExecutionId && success) {
            //update ScheduledExecution statistics for successful execution
            def time = dateCompleted.time - execution.dateStarted.time
            def savedJobState = false
            withRetry(
                statsRetryMax,
                statsRetryDelay,
                "Execution ${execution.id} update job stats (${scheduledExecutionId}):",
                executionService.&isApplicationShutdown
            ) {
                savedJobState = executionService.updateScheduledExecStatistics(scheduledExecutionId, execution.id, time)
                savedJobState
            }
            if (!savedJobState) {
                log.error("ExecutionJob: Failed to update job statistics for ${execution.id}, after retrying ${statsRetryMax} times")
            }

        }
        return saveStateComplete
    }

    @Transactional
    Execution fetchExecution(def id) {
        def execution = Execution.get(id)
        if (execution != null) {
            execution.refresh()
        }

        return execution
    }

    def ScheduledExecution fetchScheduledExecution(def jobDataMap) {
        def seid = jobDataMap.get("scheduledExecutionId")
        def ScheduledExecution se=null
        ScheduledExecution.withNewSession {
            se = ScheduledExecution.get(seid)
            if(se){
                se.refreshOptions() //force fetch options and option values before return object
            }
        }

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

    def fetchJobSchedulesService(jobDataMap){
        def es = jobDataMap.get("jobSchedulesService")
        if (es==null) {
            throw new RuntimeException("JobSchedulesService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof JobSchedulesService)) {
            throw new RuntimeException("JobDataMap contained invalid JobSchedulesService type: " + es.getClass().getName())
        }
        return es
    }

    /**
     * Return evaluated timeout duration, or -1 if not set
     * @return
     */
    long getNotifyAvgDurationThreshold(String notifyAvgDurationThreshold, long averageDuration, Map<String, Map<String, String>> dataContext){

        if(null==notifyAvgDurationThreshold){
            return averageDuration
        }

        if (notifyAvgDurationThreshold.contains('${')) {
            //replace data references
            notifyAvgDurationThreshold = DataContextUtils.replaceDataReferencesInString(notifyAvgDurationThreshold,dataContext)
        }

        //add Threshold for avg notification
        def jobAverageDurationFinal = averageDuration

        if (notifyAvgDurationThreshold?.contains('%')) {
            def numberList = notifyAvgDurationThreshold.findAll( /-?\d+\.\d*|-?\d*\.\d+|-?\d+/ )
            def percentageValue = 0
            if(numberList.size() == 1) {
                percentageValue = numberList.get(0)?.toInteger()
            }

            jobAverageDurationFinal = averageDuration + (averageDuration * (percentageValue / 100))

        }else {
            if (notifyAvgDurationThreshold?.contains('+')) {
                def avgDurationThresholdValue = Sizes.parseTimeDuration(notifyAvgDurationThreshold.replace("+", ""), TimeUnit.MILLISECONDS)
                jobAverageDurationFinal = averageDuration + avgDurationThresholdValue
            } else {
                jobAverageDurationFinal = Sizes.parseTimeDuration(notifyAvgDurationThreshold, TimeUnit.MILLISECONDS)
                if(jobAverageDurationFinal==0){
                    jobAverageDurationFinal = averageDuration
                }
            }
        }

        return jobAverageDurationFinal
    }
}

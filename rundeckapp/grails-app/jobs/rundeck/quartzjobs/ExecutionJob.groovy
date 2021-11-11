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
import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.quartz.InterruptableJob
import org.quartz.JobDataMap
import org.quartz.JobExecutionContext
import org.rundeck.util.Sizes
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.*
import rundeck.services.execution.ThresholdValue
import rundeck.services.logging.LoggingThreshold

import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class ExecutionJob implements InterruptableJob {

    public static final int DEFAULT_STATS_RETRY_MAX = 5
    public static final long DEFAULT_STATS_RETRY_DELAY = 1000
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
        if(grailsApplication?.config?.getProperty("rundeck.execution.finalize.retryMax", Integer.class)){
            finalizeRetryMax= grailsApplication.config.getProperty("rundeck.execution.finalize.retryMax", Integer.class)
        }
        if(grailsApplication?.config?.getProperty("rundeck.execution.finalize.retryDelay", Integer.class)){
            finalizeRetryDelay= grailsApplication.config.getProperty("rundeck.execution.finalize.retryDelay", Integer.class)
        }
        if(grailsApplication?.config?.getProperty("rundeck.execution.stats.retryMax", Integer.class)){
            statsRetryMax= grailsApplication.config.getProperty("rundeck.execution.stats.retryMax", Integer.class)
        }
        if(grailsApplication?.config?.getProperty("rundeck.execution.stats.retryDelay", Integer.class)){
            statsRetryDelay= grailsApplication.config.getProperty("rundeck.execution.stats.retryDelay", Integer.class)
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
        RunContext initMap
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
        executionId = initMap.executionId ?: initMap.execution?.id
        if(initMap.jobShouldNotRun){
            log.info(initMap.jobShouldNotRun)
            return
        }


        def beforeExec = initMap.jobSchedulerService.beforeExecution(
            initMap.execution.asReference(),
            context.mergedJobDataMap,
            initMap.authContext
        )
        if (beforeExec == JobScheduleManager.BeforeExecutionBehavior.skip) {
            return
        }
        RunResult result = null
        def statusString=null
        try {
            if(!wasInterrupted){
                result = executeCommand(initMap)
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
                initMap.temp,
                statusString,
                initMap.scheduledExecutionId ? initMap.scheduledExecutionId : -1L,
                initMap,
                result?.execmap
        )
        initMap.jobSchedulerService.afterExecution(initMap.execution.asReference(), context.mergedJobDataMap, initMap.authContext)
    }

    public void interrupt(){
        wasInterrupted=true;
    }
    @CompileStatic
    static class RunContext{
        boolean temp
        long executionId
        ScheduledExecution scheduledExecution
        long scheduledExecutionId
        ExecutionService executionService
        ExecutionUtilService executionUtilService
        FrameworkService frameworkService
        AuthContextProvider authContextProvider
        JobSchedulesService jobSchedulesService
        JobSchedulerService jobSchedulerService
        long timeout
        Execution execution
        IFramework framework
        UserAndRolesAuthContext authContext
        Map secureOpts
        Map secureOptsExposed
        GString jobShouldNotRun
    }

    @CompileStatic
    RunContext initialize(JobExecutionContext context, JobDataMap jobDataMap) {
        def initMap = new RunContext()
        initMap.temp = "true" == jobDataMap.get("isTempExecution")
        def executionId = jobDataMap.get("executionId")
        if (initMap.temp) {
            //temp execution, means no associated ScheduledExecution object
            if (!executionId) {
                throw new RuntimeException("executionId was not found in job data map for temporary execution")
            }
            if (executionId instanceof String) {
                initMap.executionId = Long.parseLong(executionId)
            } else if (executionId instanceof Long) {
                initMap.executionId = executionId
            }
        } else {
            initMap.scheduledExecution = fetchScheduledExecution(jobDataMap)
            if (!initMap.scheduledExecution) {
                throw new RuntimeException("scheduledExecution data was not found in job data map")
            }
            initMap.scheduledExecutionId = initMap.scheduledExecution.id
        }

        initMap.executionService = requireEntry(jobDataMap, "executionService", ExecutionService)
        initMap.executionUtilService = requireEntry(jobDataMap, "executionUtilService", ExecutionUtilService)
        initMap.frameworkService = requireEntry(jobDataMap, "frameworkService", FrameworkService)
        initMap.authContextProvider = requireEntry(jobDataMap, "authContextProvider", AuthContextProvider)
        initMap.jobSchedulesService = requireEntry(jobDataMap, "jobSchedulesService", JobSchedulesService)
        initMap.jobSchedulerService = requireEntry(jobDataMap, "jobSchedulerService", JobSchedulerService)
        if (initMap.scheduledExecution?.timeout){
            initMap.timeout = initMap.scheduledExecution.timeoutDuration
        }

        if(initMap.temp){
            //an adhoc execution without associated job
            initMap.execution = fetchExecution(initMap.executionId)
            if (!initMap.execution) {
                throw new RuntimeException("failed to lookup Exception object from job data map: id: ${initMap.executionId}")
            }
            initMap.execution.refresh()
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            initMap.authContext=requireEntry(jobDataMap,'authContext',UserAndRolesAuthContext)
        }else if(executionId){
            //a job execution invoked by a user
            if (executionId instanceof String) {
                initMap.executionId = Long.parseLong(executionId)
            } else if (executionId instanceof Long) {
                initMap.executionId = executionId
            }
            initMap.secureOpts = getEntry(jobDataMap, "secureOpts", Map)
            initMap.secureOptsExposed = getEntry(jobDataMap, "secureOptsExposed", Map)
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
            def jobArguments=FrameworkService.parseOptsFromString(initMap.execution?.argString)
            if (initMap.scheduledExecution?.timeout && initMap.scheduledExecution?.timeout?.contains('${')) {
                def timeout = DataContextUtils.replaceDataReferencesInString(initMap.scheduledExecution?.timeout,
                        DataContextUtils.addContext("option", jobArguments, null))
                initMap.timeout = timeout ? Sizes.parseTimeDuration(timeout) : -1
            }
            FrameworkService frameworkService = initMap.frameworkService
            initMap.framework = frameworkService.rundeckFramework
            initMap.authContext = requireEntry(jobDataMap, 'authContext', UserAndRolesAuthContext)
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
            initMap.authContext = initMap.authContextProvider.getAuthContextForUserAndRolesAndProject(
                    initMap.scheduledExecution.user,
                    rolelist,
                    project
            )
            initMap.secureOptsExposed = initMap.executionService.selectSecureOptionInput(initMap.scheduledExecution,[:],true)
            def inputMap=[executionType:'scheduled']
            def triggerData = context?.trigger?.jobDataMap?.get('scheduleArgs')
            if(triggerData){
                inputMap.argString = triggerData
            }
            initMap.execution = initMap.executionService.createExecution(initMap.scheduledExecution, initMap.authContext, null, inputMap)
        }
        if (!initMap.authContext) {
            throw new RuntimeException("authContext could not be determined")
        }
        return initMap
    }


    @CompileStatic
    static class RunResult{
        boolean success
        ExecutionService.AsyncStarted execmap
        WorkflowExecutionResult result
    }
    @CompileStatic
    RunResult executeCommand(RunContext runContext) {
        def success = true
        ExecutionService.AsyncStarted execmap
        try {
            execmap = runContext.executionService.executeAsyncBegin(
                runContext.framework,
                runContext.authContext,
                runContext.execution,
                runContext.scheduledExecution,
                runContext.secureOpts,
                runContext.secureOptsExposed
            )

        } catch (Exception e) {
            log.error("Execution ${runContext.execution.id} failed to start: " + e.getMessage(), e)
            throw e
        }
        if (!execmap) {
            //failed to start
            return new RunResult(success: false)
        }
        def timeoutms = 1000 * runContext.timeout
        def shouldCheckTimeout = timeoutms > 0
        long startTime = System.currentTimeMillis()
        int killcount = 0;
        def killLimit = 100
        def WorkflowExecutionServiceThread thread = execmap.thread
        def Consumer<Long> periodicCheck = execmap.periodicCheck
        def ThresholdValue threshold = execmap.threshold
        long jobAverageDuration=0
        if(runContext.scheduledExecution){
            ScheduledExecution.withTransaction {
                jobAverageDuration = runContext.scheduledExecution.averageDuration?:0
            }
        }


        def boolean avgNotificationSent = false
        def boolean stop=false


        def jobAverageDurationFinal = getNotifyAvgDurationThreshold(runContext.scheduledExecution?.notifyAvgDurationThreshold?:"0",
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
                    runContext.executionService.avgDurationExceeded(
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


        Retried retried = withRetry(
            finalizeRetryMax,
            finalizeRetryDelay,
            "Execution ${runContext.execution.id} finishExecution:",
            runContext.executionService.&isApplicationShutdown
        ) {
            runContext.executionUtilService.finishExecution(execmap)
            true
        }
        if (!retried.complete && retried.caught) {
            throw new RuntimeException("Execution ${runContext.execution.id} failed: " + retried.caught.getMessage(), retried.caught)
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

        return new RunResult(success: success && thread.isSuccessful(), execmap: execmap, result: thread.result)
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

    @CompileStatic
    static class Retried{
        boolean complete
        Throwable caught
    }
    @CompileStatic
    Retried withRetry(int max, long sleep, String identity, Closure shortcircuit = null, Closure action) {
        int count=0
        boolean complete=false
        float backoffmult = 1.5
        def jitter ={
            Math.floor(Math.random()*(sleep))
        }
        long newsleep= (long) (sleep + jitter())
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
                newsleep = (long) Math.floor(newsleep* backoffmult)
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
        return new Retried(complete: complete, caught: caught)
    }

    @CompileStatic
    def saveState(
        JobDataMap jobDataMap,
        ExecutionService executionService,
        Execution execution,
        boolean success,
        boolean _interrupted,
        boolean timedOut,
        boolean isTemp,
        String statusString,
        long scheduledExecutionId = -1,
        RunContext initMap,
        ExecutionService.AsyncStarted execmap
    )
    {
        Map<String, NodeStepResult> failedNodes = execmap?.noderecorder?.getFailedNodes()
        Set<String> succeededNodes = execmap?.noderecorder?.getSuccessfulNodes()

        if(wasThreshold && execmap?.threshold?.action==LoggingThreshold.ACTION_HALT){
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
        Map retryContext = [
                user             : execution.user,
                authContext      : jobDataMap?.get("authContext") ?: initMap?.authContext,
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
            Retried retried = withRetry(
                finalizeRetryMax,
                finalizeRetryDelay,
                "Execution ${execution.id} save result status:",
                executionService.&isApplicationShutdown,
                action
            )
            saveStateComplete=retried.complete
            if (!saveStateComplete) {
                execution.refresh()
                log.error("ExecutionJob: Failed to save execution state for ${execution.id}, after retrying ${finalizeRetryMax} times: ${retried.caught}")
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
    @CompileStatic
    Execution fetchExecution(Long id) {
        def execution = Execution.get(id)
        if (execution != null) {
            execution.refresh()
        }

        return execution
    }

    @CompileStatic
    def ScheduledExecution fetchScheduledExecution(JobDataMap jobDataMap) {
        String seid = requireEntry(jobDataMap, "scheduledExecutionId", String)
        def ScheduledExecution se=null
        se = ScheduledExecution.get(Long.parseLong(seid))
        if(se){
            se.refreshOptions() //force fetch options and option values before return object
        }

        if (!se) {
            throw new RuntimeException("failed to lookup scheduledException object from job data map: id: ${seid}")
        }
        if (! se instanceof ScheduledExecution) {
            throw new RuntimeException("JobDataMap contained invalid ScheduledExecution type: " + se.getClass().getName())
        }
        return se
    }


    @CompileStatic
    public <T> T requireEntry(Map jobDataMap, String name, Class<T> type) {
        getEntry(jobDataMap, name, type, true)
    }

    @CompileStatic
    public <T> T getEntry(Map jobDataMap, String name, Class<T> type, boolean require=false) {
        def es = jobDataMap.get(name)
        if (es == null) {
            if(require){
                throw new RuntimeException("$name could not be retrieved from JobDataMap!")
            }
            return null
        }
        if (! (type.isInstance(es))) {
            throw new RuntimeException("JobDataMap value $name contained invalid ${type.name} type: " + es.getClass().getName())
        }
        type.cast(es)
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

import org.quartz.JobExecutionContext

import org.quartz.JobDataMap

import com.dtolabs.rundeck.core.common.Framework
import org.quartz.InterruptableJob

import com.dtolabs.rundeck.core.execution.workflow.NodeRecorder
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService

class ExecutionJob implements InterruptableJob {

//    def Logger log = Logger.getLogger(ExecutionJob.class)
//    def ScheduledExecution scheduledExecution
//    def scheduledExecutionId
//    def executionId

//    def Execution execution

//    def ExecutionService executionService
//    def String adbase
//    def Framework framework
//    def isTemp
    def boolean _interrupted
//    def boolean success

    static triggers = {
        /** define no triggers here */
    }
    // Implements the Job interface, execute
    void execute(JobExecutionContext context) {
        def boolean success=false
        def Map initMap
        try{
            initMap= initialize(context)
        }catch(Throwable t){
            log.error("Unable to start Job execution: ${t.message?t.message:'no message'}",t)
            return
        }
        def result
        try {
            if(!_interrupted){
                result=executeCommand(initMap.executionService,initMap.execution,initMap.framework,initMap.scheduledExecution, initMap.extraParams, initMap.extraParamsExposed)
                success=result.success
            }
        }catch(Throwable t){
            log.error("Failed executing Job: ${t.message?t.message:'no message'}",t)
        }
        try{
            saveState(initMap.executionService, initMap.execution ? initMap.execution : (Execution) null, success,
                _interrupted, initMap.isTemp, initMap.scheduledExecutionId ? initMap.scheduledExecutionId : -1L,result?.execmap)
        }catch(Throwable t){
            log.error("Unable to save Job execution state: ${t.message?t.message:'no message'}",t)
        }
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

    public void interrupt(){
        _interrupted=true;
    }

    def initialize(JobExecutionContext context) {
        def initMap=[:]
        def jobDetail = context.getJobDetail()
        def jobDataMap = jobDetail.getJobDataMap()
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
        initMap.executionService = fetchExecutionService(jobDataMap)
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
            def subject = jobDataMap.get("userSubject")
            if(subject){
                initMap.framework = FrameworkService.getFrameworkForUserAndSubject(initMap.execution.user, subject, initMap.adbase)
            }else{
                def roles = jobDataMap.get("userRoles")
                if (null == roles) {
                    throw new RuntimeException("userRoleList not found in job data map")
                }
                def rolelist = Arrays.asList(roles.split(","))
                initMap.framework = FrameworkService.getFrameworkForUserAndRoles(initMap.execution.user, rolelist, initMap.adbase)
            }
        }else{
            if(jobDataMap.get("executionId")){
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
                def subject = jobDataMap.get("userSubject")
                if (subject) {
                    initMap.framework = FrameworkService.getFrameworkForUserAndSubject(initMap.execution.user, subject, initMap.adbase)
                } else {
                    def roles = jobDataMap.get("userRoles")
                    if (null == roles) {
                        throw new RuntimeException("userRoleList not found in job data map")
                    }
                    def rolelist = Arrays.asList(roles.split(","))
                    initMap.framework = FrameworkService.getFrameworkForUserAndRoles(initMap.execution.user, rolelist, initMap.adbase)
                }
            }else{
                initMap.framework = FrameworkService.getFrameworkForUserAndRoles(initMap.scheduledExecution.user,initMap.scheduledExecution.userRoles,initMap.adbase)
                initMap.execution = initMap.executionService.createExecution(initMap.scheduledExecution, initMap.framework,initMap.scheduledExecution.user)
            }
        }
        return initMap
    }

    def executeCommand(ExecutionService executionService,Execution execution, Framework framework, ScheduledExecution scheduledExecution=null, Map extraParams=null, Map extraParamsExposed=null) {
        def success=false
        def execmap
        try{
            execmap= executionService.executeAsyncBegin(framework,execution,scheduledExecution,extraParams, extraParamsExposed)
        }catch(Exception e){
            log.error("Execution failed: "+e.getMessage(), e)
            throw e
        }
        if(!execmap){
            //failed to start
            return [success:false]
        }

        int killcount=0;
        while(execmap.thread.isAlive()){
            try{
                execmap.thread.join(1000)
            }catch(InterruptedException e){
                //do nada
            }
            if (_interrupted) {
                if(killcount<100){
                    execmap.thread.abort()
                    Thread.yield();
                    killcount++;
                }else{
                    execmap.thread.stop()
                }

            }
        }


        try {
            success = executionService.executeAsyncFinish(execmap)
        } catch (Exception exc) {
            throw new RuntimeException("Execution failed: "+exc.getMessage(), exc)
        }
        log.debug("ExecutionJob: execution successful? " + success +", interrupted? "+_interrupted)
        return [success:success,execmap:execmap]

    }

    def saveState(ExecutionService executionService,Execution execution, boolean success, boolean _interrupted, boolean isTemp, long scheduledExecutionId=-1, Map execmap=null) {
        Map<String,Object> failedNodes=extractFailedNodes(execmap)
        if(isTemp){
            executionService.saveExecutionState(
                            null,
                            execution.id,
                                [
                                status:String.valueOf(success),
                                dateCompleted:new Date(),
                                cancelled:_interrupted,
                                failedNodes:failedNodes?.keySet(),
                                failedNodesMap:failedNodes,
                                ],
                            execmap
                            )

        }else{
            executionService.saveExecutionState(
                scheduledExecutionId,
                execution.id,
                    [
                    status:String.valueOf(success),
                    dateCompleted:new Date(),
                    cancelled:_interrupted,
                    failedNodes:failedNodes?.keySet(),
                    failedNodesMap: failedNodes,
                    ],
                    execmap
                )

        }
    }

    def ScheduledExecution fetchScheduledExecution(JobDataMap jobDataMap) {
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

    def ExecutionService fetchExecutionService(JobDataMap jobDataMap) {
        def es = jobDataMap.get("executionService")
        if (!es) {
            throw new RuntimeException("ExecutionService could not be retrieved from JobDataMap!")
        }
        if (! es instanceof ExecutionService) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionService type: " + se.getClass().getName())
        }
        return es
        
    }
}

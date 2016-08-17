package rundeck.quartzjobs


import org.quartz.JobExecutionContext
import rundeck.Messaging
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.MessagingService
import rundeck.services.ScheduledExecutionService


class HeartBeatJob {
    def MessagingService messagingService
    def FrameworkService frameworkService
    def ScheduledExecutionService scheduledExecutionService
    public static final long REPEAT_INTERVAL_SEC = 300
    public static final long REPEAT_INTERVAL_MS = REPEAT_INTERVAL_SEC*1000


    def grailsApplication
    static triggers = {
        simple repeatInterval: REPEAT_INTERVAL_MS
    }
    void execute(JobExecutionContext context) {
        if(frameworkService.isClusterModeEnabled()){
            //I'm alive heartbeat
            messagingService.generateNodeMessage(frameworkService.serverUUID)
            //check job with !scheduleOwnerClaimed
            List<Messaging> msgs = messagingService.getJobMessages(frameworkService.serverUUID)
            msgs.each {it->
                ScheduledExecution se = ScheduledExecution.findById(it.scheduledExecutionId)
                if(!se.scheduleOwnerClaimed){
                    def oldSched = scheduledExecution.scheduled
                    def oldJobName = scheduledExecution.generateJobScheduledName()
                    def oldJobGroup = scheduledExecution.generateJobGroupName()
                    scheduledExecutionService.rescheduleJob(se,oldSched,oldJobName,oldJobGroup)
                    se.scheduleOwnerClaimed = true
                    se.save()
                }
            }

        }
    }
}

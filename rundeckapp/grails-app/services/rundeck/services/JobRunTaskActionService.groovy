package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.server.plugins.trigger.action.JobRunTaskAction
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.ActionFailed
import org.rundeck.core.tasks.TaskTrigger
import org.rundeck.core.tasks.TaskActionHandler

class JobRunTaskActionService implements TaskActionHandler<RDTaskContext> {
    static transactional = false
    JobStateService jobStateService


    @Override
    Map performTaskAction(
        String taskId,
        RDTaskContext contextInfo,
        Map triggerMap,
        TaskTrigger trigger,
        TaskAction action
    ) throws ActionFailed {
        log.debug("JobRunTaskActionService: performTaskAction: $taskId, $triggerMap, $trigger, $action")
        JobRunTaskAction runAction = (JobRunTaskAction) action

        AuthContext auth = contextInfo.authContext

        try {
            JobReference jobReference = jobStateService.jobForID(auth, runAction.jobId, contextInfo.project)
            ExecutionReference exec = jobStateService.startJob(
                auth,
                jobReference,
                runAction.optionData ?: [:],
                runAction.filter,
                runAction.asUser
            )
            log.debug("JobRunTaskActionService: startedJob result: ${exec.id}")
            return [execId: exec.id, associatedType: 'Execution', associatedId: exec.id]
        } catch (JobNotFound nf) {
            throw new ActionFailed("Job ${runAction.jobId} was not found", nf)
        }

        null
    }

    @Override
    boolean handlesAction(TaskAction action, RDTaskContext contextInfo) {
        return action instanceof JobRunTaskAction
    }
}

package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.server.plugins.tasks.action.JobRunTaskAction
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.ActionFailed
import org.rundeck.core.tasks.TaskManager
import org.rundeck.core.tasks.TaskTrigger
import org.rundeck.core.tasks.TaskActionHandler

@Plugin(name = 'JobRunTaskActionService', service = 'TaskActionHandler')
class JobRunTaskActionService implements TaskActionHandler<RDTaskContext> {
    static transactional = false
    JobStateService jobStateService


    @Override
    Map performTaskAction(
        RDTaskContext contextInfo,
        Map triggerMap,
        Map userData,
        Map conditionData,
        TaskTrigger trigger,
        TaskAction action,
        TaskManager<RDTaskContext> manager
    ) throws ActionFailed {
        log.debug("JobRunTaskActionService: performTaskAction: ${contextInfo.taskId}, $triggerMap, $trigger, $action")
        JobRunTaskAction runAction = (JobRunTaskAction) action

        AuthContext auth = contextInfo.authContext

        //TODO: pass task/trigger/condition data into exec context
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

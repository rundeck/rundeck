package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.server.plugins.trigger.action.JobRunTriggerAction
import org.rundeck.core.triggers.Action
import org.rundeck.core.triggers.Condition
import org.rundeck.core.triggers.TriggerActionHandler

class JobRunTriggerActionService implements TriggerActionHandler<RDTriggerContext> {
    static transactional = false
    JobStateService jobStateService
    FrameworkService frameworkService

    @Override
    void performTriggerAction(String triggerId, RDTriggerContext contextInfo, Map conditionMap, Condition condition, Action action) {
        log.error("TODO: JobRunTriggerActionService: performTriggerAction: $triggerId, $conditionMap, $condition, $action")
        JobRunTriggerAction runAction = (JobRunTriggerAction) action

        AuthContext auth = contextInfo.authContext

        JobReference jobReference = jobStateService.jobForID(auth, runAction.jobId, contextInfo.project)
        String execId = jobStateService.startJob(auth, jobReference, runAction.argString, runAction.filter, runAction.asUser)

        log.error("JobRunTriggerActionService: startedJob result: $execId")
    }

    @Override
    boolean handlesAction(Action action, RDTriggerContext contextInfo) {
        return action instanceof JobRunTriggerAction
    }
}

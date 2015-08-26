package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.SynchState

/**
 * Created by greg on 8/24/15.
 */
class JobGitState implements JobState {
    SynchState synchState
    Map stateMeta

    @Override
    public String toString() {
        return "JobGitState{" +
                "synchState=" + synchState +
                ", stateMeta=" + stateMeta +
                '}';
    }
}

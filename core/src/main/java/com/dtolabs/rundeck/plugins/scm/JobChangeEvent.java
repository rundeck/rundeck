package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.jobs.JobReference;
import com.dtolabs.rundeck.core.jobs.JobRevReference;

/**
 * Created by greg on 4/28/15.
 */
public interface JobChangeEvent {
    /**
     * Job was modified in Rundeck
     */
    public enum JobChangeEventType {
        CREATE,
        MODIFY,
        DELETE,
        MODIFY_RENAME
    }
    JobChangeEventType getEventType();
    JobRevReference getJobReference();
    JobReference getOriginalJobReference();

}

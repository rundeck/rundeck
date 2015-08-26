package rundeck.services

import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent

/**
 * Created by greg on 4/28/15.
 */
class StoredJobChangeEvent implements JobChangeEvent{
    JobChangeEvent.JobChangeEventType eventType
    JobRevReference jobReference
    JobReference originalJobReference
}

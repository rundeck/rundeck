package rundeck.services.jobs

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobState

/**
 * Extends {@link com.dtolabs.rundeck.core.jobs.JobService} interface to include an AuthContext
 */
interface AuthorizingJobService {

    JobReference jobForID(AuthContext auth, String uuid, String project) throws JobNotFound;

    JobReference jobForName(AuthContext auth, String name, String project) throws JobNotFound;

    JobReference jobForName(AuthContext auth, String group, String name, String project) throws JobNotFound;

    JobState getJobState(AuthContext auth, JobReference jobReference) throws JobNotFound;
}
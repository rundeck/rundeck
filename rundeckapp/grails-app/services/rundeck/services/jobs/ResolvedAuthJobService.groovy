package rundeck.services.jobs

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState

/**
 * Created by greg on 2/3/15.
 */
class ResolvedAuthJobService implements JobService {
    AuthorizingJobService authJobService
    AuthContext authContext

    @Override
    JobReference jobForID(String uuid, String project) throws JobNotFound {
        authJobService.jobForID(authContext, uuid, project)
    }

    @Override
    JobReference jobForName(String name, String project) throws JobNotFound {
        authJobService.jobForName(authContext, name, project)
    }

    @Override
    JobReference jobForName(String group, String name, String project) throws JobNotFound {
        authJobService.jobForName(authContext, group, name, project)
    }

    @Override
    JobState getJobState(JobReference jobReference) throws JobNotFound {
        authJobService.getJobState(authContext, jobReference)
    }
}

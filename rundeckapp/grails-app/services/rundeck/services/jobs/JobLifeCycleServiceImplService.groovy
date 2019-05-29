package rundeck.services.jobs

import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.jobs.JobLifeCycleEvent
import com.dtolabs.rundeck.core.jobs.JobLifeCycleService
import com.dtolabs.rundeck.core.jobs.JobLifeCycleStatus
import rundeck.services.JobLifeCyclePluginService


class JobLifeCycleServiceImplService implements JobLifeCycleService {

    JobLifeCyclePluginService jobLifeCyclePluginService

    JobLifeCycleStatus beforeJobStarts(JobLifeCycleEvent event) throws JobLifeCycleException{
        jobLifeCyclePluginService.beforeJobStarts(event)
    }

    JobLifeCycleStatus afterJobEnds(JobLifeCycleEvent event) throws JobLifeCycleException{
        jobLifeCyclePluginService.afterJobEnds(event)
    }

}
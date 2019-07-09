package rundeck.services.jobs

import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.jobs.JobEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.jobs.CoreJobPluginService
import rundeck.services.JobPluginService


class JobPluginServiceImplService implements CoreJobPluginService {

    JobPluginService jobPluginService

    JobEventStatus beforeJobStarts(JobEvent event) throws JobPluginException{
        jobPluginService.beforeJobStarts(event)
    }

    JobEventStatus afterJobEnds(JobEvent event) throws JobPluginException{
        jobPluginService.afterJobEnds(event)
    }

}
package rundeck.services.jobs

import com.dtolabs.rundeck.core.execution.JobLifeCycleException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobLifeCycleService
import com.dtolabs.rundeck.core.jobs.JobStatus
import com.dtolabs.rundeck.core.logging.LoggingManager
import rundeck.services.JobLifeCyclePluginService


class JobLifeCycleServiceImplService implements JobLifeCycleService {

    JobLifeCyclePluginService jobLifeCyclePluginService

    JobStatus onBeforeJobStart(WorkflowExecutionItem item, StepExecutionContext executionContext,
                               LoggingManager workflowLogManager) throws JobLifeCycleException{
        jobLifeCyclePluginService.onBeforeJobStart(item, executionContext, workflowLogManager);
    }

}
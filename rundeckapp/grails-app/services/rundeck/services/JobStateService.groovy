package rundeck.services

import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.jobs.JobState
import grails.transaction.Transactional
import rundeck.Execution
import rundeck.ScheduledExecution

@Transactional
class JobStateService implements JobService {

    @Override
    JobReference jobForID(String uuid, String project) throws JobNotFound {
        def job = ScheduledExecution.getByIdOrUUID(uuid)
        if (null == job || job.project != project) {
            throw new JobNotFound("Not found", uuid, project)
        }
        return new JobReferenceImpl(id: job.extid, jobName: job.jobName, groupPath: job.groupPath, project: job.project)
    }

    @Override
    JobReference jobForName(String name, String project) throws JobNotFound {
        def group = null
        if (name.lastIndexOf('/') >= 0 && name.lastIndexOf('/') < name.size() - 1) {
            group = name.substring(0, name.lastIndexOf('/'))
            name = name.substring(name.lastIndexOf('/') + 1)
        }
        return jobForName(group, name, project)
    }

    @Override
    JobReference jobForName(String group, String name, String project) throws JobNotFound {
        def job = ScheduledExecution.findByProjectAndJobNameAndGroupPath(project, name, group)
        if (null == job) {
            throw new JobNotFound("Not found", name, group, project)
        }
        return new JobReferenceImpl(id: job.extid, jobName: job.jobName, groupPath: job.groupPath, project: job.project)
    }

    @Override
    JobState getJobState(JobReference jobReference) throws JobNotFound{
        def job = ScheduledExecution.getByIdOrUUID(jobReference.id)
        if (null == job) {
            throw new JobNotFound("Not found", jobReference.id, jobReference.project)
        }
        def running = Execution.findAllByScheduledExecutionAndDateCompletedIsNull(job)
        def lastExec = Execution.findAllByScheduledExecutionAndDateCompletedIsNotNull(job, [order: 'desc', sort: 'dateCompleted', max: 1])
        def previousState = null
        if (lastExec.size() == 1) {
            previousState = ExecutionState.valueOf(ExecutionService.getExecutionState(lastExec[0]).replaceAll('[-]','_'))
        }
        def runningIds = new HashSet<String>(running.collect { it.id.toString() })
        new JobStateImpl(
                running: running.size() > 0,
                runningExecutionIds: runningIds,
                previousExecutionState: previousState
        )
    }
}

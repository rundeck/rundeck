package org.rundeck.app.data.providers.v1.job;

import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.job.JobDataSummary;
import org.rundeck.app.data.model.v1.page.Page;
import org.rundeck.app.data.model.v1.page.Pageable;
import org.rundeck.app.data.model.v1.query.JobQueryInputData;

public interface JobQueryProvider {

    /**
     * Return all jobs where the 'scheduled' flag is set to true
     * @param project - optional the project that owns the job
     * @param serverNodeUuid - optional the server the job is currently assigned to
     * @param pageable
     * @return
     */
    Page<JobData> getAllScheduledJobs(String project, String serverNodeUuid, Pageable pageable);
    Page<JobDataSummary> listJobsByProject(String project, Pageable pageable);
    Page<JobDataSummary> queryJobs(JobQueryInputData jobQueryInput);

}

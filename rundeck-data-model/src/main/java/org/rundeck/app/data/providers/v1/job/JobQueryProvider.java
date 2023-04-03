package org.rundeck.app.data.providers.v1.job;

import org.rundeck.app.data.model.v1.job.JobDataSummary;
import org.rundeck.app.data.model.v1.page.Page;
import org.rundeck.app.data.model.v1.query.JobQueryInputData;

public interface JobQueryProvider {

    Page<JobDataSummary> queryJobs(JobQueryInputData jobQueryInput);

}

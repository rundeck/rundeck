package org.rundeck.app.data.model.v1.job;

public interface JobBrowseItem
        extends JobGroupData
{
    boolean isJob();

    JobDataSummary getJobData();
}

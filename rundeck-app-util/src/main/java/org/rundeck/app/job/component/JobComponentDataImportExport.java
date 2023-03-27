package org.rundeck.app.job.component;

import org.rundeck.app.data.model.v1.job.JobData;

public interface JobComponentDataImportExport {

    String getComponentKey();

    Object importFromJobData(JobData jobData);

    void exportToJobData(JobData jobData);

}

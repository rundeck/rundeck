package org.rundeck.app.data.model.v1.execution;

import org.rundeck.app.data.model.v1.job.JobData;

import java.util.List;

public interface RdReferencedExecution {

    JobData getScheduledExecution();
    String getStatus();
    Execution getExecution();

}

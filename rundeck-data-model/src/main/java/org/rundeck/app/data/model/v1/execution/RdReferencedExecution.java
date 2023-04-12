package org.rundeck.app.data.model.v1.execution;


public interface RdReferencedExecution {

    String getStatus();
    Long getExecutionId();
    Long getScheduledExecutionJobId();

}

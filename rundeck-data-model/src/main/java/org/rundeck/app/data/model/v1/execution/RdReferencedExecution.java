package org.rundeck.app.data.model.v1.execution;


import java.io.Serializable;

public interface RdReferencedExecution {

    String getStatus();
    Serializable getExecutionId();
    Serializable getJobId();

}

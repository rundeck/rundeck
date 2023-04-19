package org.rundeck.app.data.model.v1.execution;


import java.io.Serializable;

public interface RdReferencedExecution {

    String getJobUuid();
    String getStatus();
    Serializable getExecutionId();

}

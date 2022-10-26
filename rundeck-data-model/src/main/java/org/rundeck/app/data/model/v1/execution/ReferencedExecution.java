package org.rundeck.app.data.model.v1.execution;

import java.io.Serializable;

public interface ReferencedExecution {
    Serializable getId();
    Serializable getJobId();
    Serializable getExecutionId();
    String getStatus();
}

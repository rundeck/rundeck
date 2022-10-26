package org.rundeck.app.data.model.v1.logstorage;

import org.rundeck.app.data.model.v1.execution.Execution;

import java.io.Serializable;
import java.util.Date;

public interface LogFileStorageRequest {
    Serializable getId();
    Execution getExecution();
    String getPluginName();
    String getFiletype();
    Boolean getCompleted();

    Date getDateCreated();
    Date getLastUpdated();
}

package org.rundeck.app.data.model.v1.logstorage;

import java.io.Serializable;
import java.util.Date;

public interface RdLogFileStorageRequest {
    Serializable getId();
    Serializable getExecutionId();
    String getPluginName();
    String getFiletype();
    Boolean getCompleted();

    Date getDateCreated();
    Date getLastUpdated();
}

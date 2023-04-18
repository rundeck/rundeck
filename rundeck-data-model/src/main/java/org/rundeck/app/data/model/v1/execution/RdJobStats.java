package org.rundeck.app.data.model.v1.execution;

import java.io.Serializable;

public interface RdJobStats {

    Serializable getJobUuid();
    String getContent();
    Long getVersion();
}

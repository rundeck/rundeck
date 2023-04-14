package org.rundeck.app.data.model.v1.execution;

import java.io.Serializable;

public interface RdJobStats {

    Serializable getJobId();
    String getContent();
    Long getVersion();
}

package org.rundeck.app.data.model.v1.execution;

import java.io.Serializable;

public interface RdJobStats {

    String getJobUuid();
    String getContent();
    Long getVersion();
}

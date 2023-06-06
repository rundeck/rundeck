package org.rundeck.app.data.model.v1.project;

import java.io.Serializable;
import java.util.Date;

public interface RdProject {
    Serializable getId();

    String getName();

    String getDescription();

    Date getDateCreated();

    Date getLastUpdated();

    State getState();

    enum State {
        ENABLED,
        DISABLED
    }
}

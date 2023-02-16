package org.rundeck.app.data.model.v1.pluginMeta;

import java.util.Date;

public interface RdPluginMeta {
    Long getId();
    String getKey();
    String getProject();
    String getJsonData();
    Date getDateCreated();
    Date getLastUpdated();
}

package org.rundeck.app.data.model.v1.pluginMeta;

import java.util.Date;
import java.util.Map;

public interface RdPluginMeta {
    Long getId();
    String getKey();
    String getProject();
    Date getDateCreated();
    Date getLastUpdated();
    Map<String,Object> getPluginData();
}

package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.pluginMeta.RdPluginMeta;

import java.util.List;
import java.util.Map;

public interface PluginMetaDataProvider extends DataProvider{
    RdPluginMeta findByProjectAndKey(String project, String key);
    List<RdPluginMeta> findAllByProjectAndKeyLike(String project, String key);
    void deleteByProjectAndKey(String project, String key);
    void deleteAllByProjectAndKeyLike(String project, String keyLike);
    Integer deleteAllByProject(String project);
    void setJobPluginMeta(String project, String id, String type, Map key);
}

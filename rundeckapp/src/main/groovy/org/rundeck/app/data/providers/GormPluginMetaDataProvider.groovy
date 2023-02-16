package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.pluginMeta.RdPluginMeta
import org.rundeck.app.data.providers.v1.PluginMetaDataProvider
import rundeck.PluginMeta

class GormPluginMetaDataProvider implements PluginMetaDataProvider{

    @Override
    RdPluginMeta findByProjectAndKey(String project, String key) {
        return PluginMeta.findByProjectAndKey(project, key)
    }

    @Override
    List<RdPluginMeta> findAllByProjectAndKeyLike(String project, String key) {
        return PluginMeta.findAllByProjectAndKeyLike(project, key)
    }

    @Override
    void deleteByProjectAndKey(String project, String key) {
        def found = PluginMeta.findByProjectAndKey(project, key)
        if (found) {
            found.delete(flush: true)
        }
    }

    @Override
    void deleteAllByProjectAndKeyLike(String project, String key) {
        def found = PluginMeta.findAllByProjectAndKeyLike(project, key)
        if (found) {
            found*.delete(flush: true)
        }
    }

    @Override
    Integer deleteAllByProject(String project) {
        return PluginMeta.executeUpdate('delete PluginMeta where project=:project', [project: project], [flush: true])

    }

    @Override
    Integer deleteByProjectAndDataKey(String project, String type) {
        return PluginMeta.executeUpdate('delete PluginMeta where project=:project and data_key like :data_key' , [project: project, data_key: "%/${type}"], [flush: true])
    }

    @Override
    void setJobPluginMeta(String project, String id, String key, Map metadata) {
        def found = PluginMeta.findByProjectAndKey(project, key)
        if (!found) {
            found = new PluginMeta()
            found.project = project
            found.key = key
        }
        found.setPluginData(metadata)
        found.save(flush: true)
    }
}

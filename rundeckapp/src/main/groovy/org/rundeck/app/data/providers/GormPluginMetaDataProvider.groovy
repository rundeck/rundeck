package org.rundeck.app.data.providers

import org.rundeck.app.data.model.v1.pluginMeta.RdPluginMeta
import org.rundeck.app.data.providers.v1.pluginmeta.PluginMetaDataProvider
import rundeck.PluginMeta

class GormPluginMetaDataProvider implements PluginMetaDataProvider{

    @Override
    RdPluginMeta findByProjectAndKey(String project, String key) {
        return PluginMeta.findByProjectAndKey(project, key, [cache: false])
    }

    @Override
    List<RdPluginMeta> findAllByProjectAndKeyLike(String project, String key) {
        return PluginMeta.findAllByProjectAndKeyLike(project, key, [cache: false])
    }

    @Override
    void deleteByProjectAndKey(String project, String key) {
        def found = PluginMeta.findByProjectAndKey(project, key, [cache: false])
        if (found) {
            found.delete(flush: true)
        }
    }

    @Override
    void deleteAllByProjectAndKeyLike(String project, String keyLike) {
        def found = PluginMeta.findAllByProjectAndKeyLike(project, keyLike, [cache: false])
        if (found) {
            found*.delete(flush: true)
        }
    }

    @Override
    Integer deleteAllByProject(String project) {
        // Grails 7: DataTest trait doesn't support HQL executeUpdate, use criteria instead
        def found = PluginMeta.findAllByProject(project, [cache: false])
        def count = found.size()
        found*.delete(flush: true)
        return count
    }

    @Override
    void setJobPluginMeta(String project, String key, Map metadata) {
        def found = PluginMeta.findByProjectAndKey(project, key, [cache: false])
        if (!found) {
            found = new PluginMeta()
            found.project = project
            found.key = key
        }
        found.setPluginData(metadata)
        found.save(flush: true)
    }
}

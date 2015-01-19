package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.DbStorageService

/**
 * DbStoragePluginFactory is ...
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-04-03
 */
class DbStoragePluginFactory implements FactoryBean<StoragePlugin>, InitializingBean{

    @Autowired
    DbStorageService dbStorageService;

    @Override
    StoragePlugin getObject() throws Exception {
        if (null == dbStorageService) {
            throw new IllegalArgumentException("dbStorageService is not set")
        }
        def plugin = new DbStoragePlugin()
        plugin.namespacedStorage=dbStorageService
        return plugin
    }

    @Override
    Class<?> getObjectType() {
        return StoragePlugin.class
    }

    @Override
    boolean isSingleton() {
        return false
    }

    @Override
    void afterPropertiesSet() throws Exception {

    }
}

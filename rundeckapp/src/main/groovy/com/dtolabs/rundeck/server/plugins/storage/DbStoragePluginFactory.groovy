/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

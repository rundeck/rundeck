/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.logstorage

import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.StorageService

/**
 * @author greg
 * @since 2/14/17
 */
class TreeExecutionFileStoragePluginFactory implements FactoryBean<ExecutionFileStoragePlugin>, InitializingBean {
    @Autowired
    StorageTree rundeckStorageTree

    @Override
    void afterPropertiesSet() throws Exception {

    }

    @Override
    ExecutionFileStoragePlugin getObject() throws Exception {
        if (null == rundeckStorageTree) {
            throw new IllegalArgumentException("rundeckStorageTree is not set")
        }
        def plugin = new TreeExecutionFileStoragePlugin()
        plugin.rundeckStorageTree = rundeckStorageTree
        return plugin
    }

    @Override
    Class<?> getObjectType() {
        return ExecutionFileStoragePlugin
    }

    @Override
    boolean isSingleton() {
        return false
    }
}

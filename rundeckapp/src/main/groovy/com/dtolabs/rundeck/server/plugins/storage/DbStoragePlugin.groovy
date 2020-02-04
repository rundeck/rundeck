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

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.server.storage.NamespacedStorage
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

/**
 * DbStoragePlugin uses a namespace and
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-04-03
 */
@Plugin(name = DbStoragePlugin.PROVIDER_NAME, service = ServiceNameConstants.Storage)
@PluginDescription(title = 'DB Storage', description = 'Uses DB as storage layer.')
class DbStoragePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin{
    static final PROVIDER_NAME = 'db'

    @PluginProperty(title = 'Namespace', description = 'Namespace for storage')
    String namespace;

    NamespacedStorage namespacedStorage;

    Tree<ResourceMeta> delegateTree

    public Tree<ResourceMeta> getDelegate() {
        if (null == delegateTree) {
            if (null == namespacedStorage) {
                throw new IllegalArgumentException("namespacedStorage is not set")
            }
            delegateTree = StorageUtil.resolvedTree(namespace?:null,namespacedStorage)
        }
        return delegateTree;
    }
}

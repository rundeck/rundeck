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
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.file.FileTreeUtil
import org.rundeck.storage.impl.DelegateTree

/**
 * FileStoragePlugin provides the basic file-system storage layer.
 * @author greg
 * @since 2014-02-19
 */
@Plugin(name = FileStoragePlugin.PROVIDER_NAME, service = ServiceNameConstants.Storage)
@PluginDescription(title = "Filesystem Storage", description = "Stores data on the local filesystem.")
class FileStoragePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin {
    public static final String PROVIDER_NAME = "file"
    @PluginProperty(title = "Base Directory",description = "Local base directory for file storage", required = true)
    String baseDir

    Tree<ResourceMeta> delegateTree

    public Tree<ResourceMeta> getDelegate() {
        if (null == delegateTree) {
            if(null==baseDir) {
                throw new IllegalArgumentException("baseDir is not set")
            }
            def file = new File(baseDir)
            delegateTree = FileTreeUtil.forRoot(file,StorageUtil.factory())
        }
        return delegateTree;
    }
}

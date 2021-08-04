/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.plugin.azureobjectstore

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.microsoft.azure.storage.CloudStorageAccount
import io.minio.MinioClient
import org.rundeck.plugin.azureobjectstore.directorysource.ObjectStoreDirectAccessDirectorySource
import org.rundeck.plugin.azureobjectstore.tree.ObjectStoreTree
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

import java.util.concurrent.TimeUnit

@Plugin(name = 'azure-repository-object-store', service = ServiceNameConstants.Storage)
@PluginDescription(title = 'Azure Object Storage', description = 'Use Azure object store as storage layer.')
class ObjectStorePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin {
    @PluginProperty(title = 'Bucket', description = 'Base bucket into which objects are stored')
    String bucket;
    @PluginProperty(title = 'Object Store Url', description = 'The URL endpoint of the s3 compatible service')
    String objectStoreUrl;
    @PluginProperty(title = "Storage Account", description = "Azure Storage Account")
    private String storageAccount;
    @PluginProperty(title = "Access Key", description = "Azure Storage Access Key")
    private String accessKey;
    @PluginProperty(title = "Endpoint Protocol", description = "Default Endpoint Protocol: http or https ", defaultValue = "http")
    private String defaultEndpointProtocol
    @PluginProperty(title = "Extra connection string settings", description = "Extra connection settings, see https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string#store-a-connection-string")
    private String extraConnectionSettings
    @PluginProperty(title = 'Uncached Object Lookup', description = """Use object store directly to list directory resources, check object existence, etc. 
                                                                    Depending on the directory structure and number of objects, enabling this option could have
                                                                    performance issues. This option will work better in a cluster because all servers in the cluster will
                                                                    have coordinated access to the objects managed by the plugins. NOTE: The cached object directory does
                                                                    not share the cache between servers, so it is not best to use it when operating a Rundeck cluster.""")
    boolean uncachedObjectLookup = false;
    @PluginProperty(title = "Connection Timeout", description = "Timeout in seconds for the http connection to the server (0 means no timeout)",defaultValue = "180")
    Long connectionTimeout

    Tree<ResourceMeta> delegateTree

    @Override
    Tree<ResourceMeta> getDelegate() {
        if (!delegateTree) {
            initTree()
        }
        return delegateTree
    }

    void initTree() {
        if (!bucket) {
            throw new IllegalArgumentException("bucket property is required")
        }
        if (!objectStoreUrl) {
            throw new IllegalArgumentException("objectStoreUrl property is required")
        }

        String storageConnectionString = "DefaultEndpointsProtocol="+defaultEndpointProtocol+";AccountName=" + storageAccount+ ";AccountKey=" + accessKey;

        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString)
        if(uncachedObjectLookup) {
            delegateTree = new ObjectStoreTree(account,bucket,new ObjectStoreDirectAccessDirectorySource(mClient,bucket))
        } else {
            delegateTree = new ObjectStoreTree(account, bucket)
        }
    }
}

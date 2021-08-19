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
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import org.rundeck.plugin.azureobjectstore.directorysource.AzureObjectStoreDirectAccessDirectorySource
import org.rundeck.plugin.azureobjectstore.tree.AzureObjectStoreTree
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

@Plugin(name = 'azure-repository-object-store', service = ServiceNameConstants.Storage)
@PluginDescription(title = 'Azure Object Storage', description = 'Use Azure object store as storage layer.')
class AzureObjectStorePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin {
    @PluginProperty(title = 'Container', description = 'Container into which objects are stored')
    String container;
    @PluginProperty(title = "Storage Account", description = "Azure Storage Account")
    private String storageAccount;
    @PluginProperty(title = "Access Key", description = "Azure Storage Access Key")
    private String accessKey;
    @PluginProperty(title = "Endpoint Protocol", description = "Default Endpoint Protocol: http or https ", defaultValue = "http")
    private String defaultEndpointProtocol
    @PluginProperty(title = "Extra connection string settings", description = "Extra connection settings, see https://docs.microsoft.com/en-us/azure/storage/common/storage-configure-connection-string#store-a-connection-string")
    private String extraConnectionSettings
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
        if (!container) {
            throw new IllegalArgumentException("container property is required")
        }
        if (!storageAccount) {
            throw new IllegalArgumentException("storageAccount property is required")
        }

        if (!accessKey) {
            throw new IllegalArgumentException("accessKey property is required")
        }

        String storageConnectionString = "DefaultEndpointsProtocol="+defaultEndpointProtocol+";AccountName=" + storageAccount+ ";AccountKey=" + accessKey;

        CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString)
        CloudBlobClient serviceClient = account.createCloudBlobClient();

        CloudBlobContainer blobContainer = serviceClient.getContainerReference(container)
        blobContainer.createIfNotExists()

        delegateTree = new AzureObjectStoreTree(blobContainer,new AzureObjectStoreDirectAccessDirectorySource(blobContainer))

    }
}

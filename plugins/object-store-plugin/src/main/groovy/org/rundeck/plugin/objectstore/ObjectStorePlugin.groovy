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
package org.rundeck.plugin.objectstore

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import io.minio.MinioClient
import org.rundeck.plugin.objectstore.tree.ObjectStoreTree
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

@Plugin(name = 'object', service = ServiceNameConstants.Storage)
@PluginDescription(title = 'Object Storage', description = 'Use an Amazon S3 compatible object store as storage layer.')
class ObjectStorePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin {
    @PluginProperty(title = 'Bucket', description = 'Base bucket into which objects are stored')
    String bucket;
    @PluginProperty(title = 'Object Store Url', description = 'The URL endpoint of the s3 compatible service')
    String objectStoreUrl;
    @PluginProperty(title = 'Secret Key', description = 'The secret key use by the client to connect to the service')
    String secretKey;
    @PluginProperty(title = 'Access Key', description = 'The access key use by the client to connect to the service')
    String accessKey;

    Tree<ResourceMeta> delegateTree

    @Override
    Tree<ResourceMeta> getDelegate() {
        if(!delegateTree) initTree()
        return delegateTree
    }

    void initTree() {
        if(!bucket) throw new IllegalArgumentException("bucket property is required")
        if(!objectStoreUrl) throw new IllegalArgumentException("objectStoreUrl property is required")

        MinioClient mClient = new MinioClient(objectStoreUrl, accessKey, secretKey)
        delegateTree = new ObjectStoreTree(mClient, bucket)
    }
}

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
import org.rundeck.plugin.objectstore.directorysource.ObjectStoreDirectAccessDirectorySource
import org.rundeck.plugin.objectstore.tree.ObjectStoreTree
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

import java.util.concurrent.TimeUnit

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

        MinioClient mClient = new MinioClient(objectStoreUrl, accessKey, secretKey)
        if(!connectionTimeout) connectionTimeout = 180L
        mClient.setTimeout(TimeUnit.SECONDS.toMillis(connectionTimeout),TimeUnit.SECONDS.toMillis(connectionTimeout),TimeUnit.SECONDS.toMillis(connectionTimeout))
        if(uncachedObjectLookup) {
            delegateTree = new ObjectStoreTree(mClient,bucket,new ObjectStoreDirectAccessDirectorySource(mClient,bucket))
        } else {
            delegateTree = new ObjectStoreTree(mClient, bucket)
        }
    }
}

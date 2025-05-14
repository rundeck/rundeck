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
package com.rundeck.repository.client.artifact

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.ArtifactInstaller
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.repository.RundeckHttpRepository
import com.rundeck.repository.client.util.PathUtils
import com.rundeck.repository.client.util.ResourceFactory
import org.rundeck.storage.data.DataUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class StorageTreeArtifactInstaller implements ArtifactInstaller {
    private static Logger LOG = LoggerFactory.getLogger(StorageTreeArtifactInstaller)
    private static final ResourceFactory RESOURCE_FACTORY = new ResourceFactory()
    private StorageTree storageTree
    private final String pluginPath

    StorageTreeArtifactInstaller(StorageTree storageTree, String treePath) {
        this.storageTree = storageTree
        this.pluginPath = treePath
    }

    @Override
    ResponseBatch installArtifact(final RepositoryArtifact artifact, InputStream binaryInputStream) {
        ResponseBatch batch = new ResponseBatch()
        try {
            String artifactKey = pluginPath+"/"+ artifact.getInstallationFileName()
            def resource = DataUtil.withStream(binaryInputStream, [:], RESOURCE_FACTORY)
            if(storageTree.hasResource(artifactKey)) {
                LOG.debug("Updating artifact at location: " + artifactKey)
                storageTree.updateResource(artifactKey, resource)
            } else {
                LOG.debug("Installing new artifact to location: " + artifactKey)
                storageTree.createResource(artifactKey, resource)
            }
            try {
                //some input streams must be closed manually or they will
                //retain resources
                binaryInputStream.close()
            } catch(IOException iex) { }
            batch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            LOG.error("Install failed", ex)
            batch.addMessage(new ResponseMessage(code: ResponseCodes.INSTALL_FAILED,message: ex.message))
        }

        return batch
    }
}

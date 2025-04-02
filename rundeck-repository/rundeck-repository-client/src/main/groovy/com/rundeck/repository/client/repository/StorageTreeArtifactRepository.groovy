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
package com.rundeck.repository.client.repository

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.client.manifest.StorageTreeManifestCreator
import com.rundeck.repository.client.manifest.StorageTreeManifestSource
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.PathUtils
import com.rundeck.repository.client.util.ResourceFactory
import com.rundeck.repository.client.validators.BinaryValidator
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository
import com.rundeck.repository.events.RepositoryEventEmitter
import com.rundeck.repository.events.RepositoryUpdateEvent
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.manifest.ManifestSource
import groovy.transform.PackageScope
import org.rundeck.storage.data.DataUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StorageTreeArtifactRepository implements ArtifactRepository {
    private static Logger LOG = LoggerFactory.getLogger(StorageTreeArtifactRepository)
    private static final String MEMORY_MANIFEST_SOURCE = "memory"
    static final String ARTIFACT_BASE = "/artifacts/"
    static final String BINARY_BASE = "/binary/"
    @PackageScope
    StorageTree storageTree
    RepositoryDefinition repositoryDefinition
    private final ManifestService manifestService
    private final RepositoryEventEmitter eventEmitter
    private StorageTreeManifestCreator manifestCreator
    static final ResourceFactory resourceFactory = new ResourceFactory()
    private ManifestSource manifestSource
    protected final String artifactBase
    protected final String binaryBase

    StorageTreeArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition, RepositoryEventEmitter eventEmitter) {
        this(storageTree,repositoryDefinition)
        this.eventEmitter = eventEmitter

    }

    StorageTreeArtifactRepository(StorageTree storageTree, RepositoryDefinition repositoryDefinition) {
        if(!storageTree) throw new Exception("Unable to initialize storage tree repository. No storage tree provided.")
        if(!repositoryDefinition.configProperties.storageTreePath) LOG.info("No configProperties.storageTreePath specified. Using '/'")
        this.storageTree = storageTree
        String storageTreePath = repositoryDefinition.configProperties.storageTreePath ?: "/"
        this.repositoryDefinition = repositoryDefinition
        this.artifactBase = PathUtils.composePath(storageTreePath, ARTIFACT_BASE)
        this.binaryBase = PathUtils.composePath(storageTreePath,BINARY_BASE)
        this.manifestSource = repositoryDefinition.configProperties.manifestType == MEMORY_MANIFEST_SOURCE ? new MemoryManifestSource() : new StorageTreeManifestSource(storageTree, storageTreePath)
        this.manifestService = new MemoryManifestService(manifestSource)
        manifestCreator = new StorageTreeManifestCreator(storageTree,storageTreePath)
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version = null) {
        def entry = manifestService.getEntry(artifactId)
        if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
        String artifactVer = version ?: entry.currentVersion
        ArtifactUtils.createArtifactFromYamlStream(storageTree.getResource(artifactBase+ ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)).contents.inputStream)
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        return storageTree.getResource(binaryBase+ ArtifactUtils.artifactBinaryFileName(artifactId, artifactVer, extension)).contents.inputStream
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        checkForRequiredProperty(responseBatch,artifactFileset.artifact.name,"Upload does not contain a plugin name. Please set the plugin name in the file.")
        checkForRequiredProperty(responseBatch,artifactFileset.artifact.version,"Upload does not contain a plugin version. Please set the plugin version in the file.")
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(BinaryValidator.validate(artifactFileset.artifact.artifactType, artifactFileset.artifactBinary).messages)
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(saveNewArtifact(artifactFileset.artifact).messages)
        responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        responseBatch
    }

    void checkForRequiredProperty(
            final ResponseBatch responseBatch,
            final def propVal,
            final String failedValidationResponse
    ) {
        if(!propVal) {
            responseBatch.addMessage(new ResponseMessage(code: ResponseCodes.BINARY_UPLOAD_FAILED,message: failedValidationResponse))
        }
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        ResponseBatch response = new ResponseBatch()
        String artifactPath = artifactBase + artifact.getArtifactMetaFileName()
        if(storageTree.hasResource(artifactPath)) {
            response.messages.add(new ResponseMessage(code: ResponseCodes.SUCCESS,message:"Artifact already exists"))

            return response
        }

        Map meta = [:]
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            ArtifactUtils.saveArtifactToOutputStream(artifact,baos)
            def resource = DataUtil.withStream(new ByteArrayInputStream(baos.toByteArray()), meta, resourceFactory)
            def created = storageTree.createResource(artifactPath, resource)
            created.contents.inputStream.close() //must close the input stream, otherwise it will leak

            response.messages.add(new ResponseMessage(code: ResponseCodes.SUCCESS))
            //recreate manifest
            recreateAndSaveManifest()
            //emit event that repo was updated
            if(eventEmitter) {
                eventEmitter.emit(new RepositoryUpdateEvent(repositoryDefinition.repositoryName, artifactId))
            }

        } catch (Exception ex) {
            ex.printStackTrace()
            response.messages.add(new ResponseMessage(code:ResponseCodes.SERVER_ERROR,message: ex.message))
        }
        response
    }

    @PackageScope
    ResponseBatch uploadArtifactBinary(final RundeckRepositoryArtifact artifact, final InputStream artifactBinaryInputStream) {
        ResponseBatch response = new ResponseBatch()
        String binaryPath = binaryBase+ artifact.getArtifactBinaryFileName()
        if(storageTree.hasResource(binaryPath)) {
            response.messages.add(new ResponseMessage(code: ResponseCodes.SUCCESS, message: "Binary already exists"))
            return response
        }
        Map meta = [:]
        try {
            def resource = DataUtil.withStream(artifactBinaryInputStream, meta, resourceFactory)
            def created = storageTree.createResource(binaryPath, resource)
            created.contents.inputStream.close() //must close the input stream, otherwise it will leak

            response.messages.add(new ResponseMessage(code:ResponseCodes.SUCCESS))
        } catch (Exception ex) {
            ex.printStackTrace()
            response.messages.add(new ResponseMessage(code:ResponseCodes.SERVER_ERROR,message: ex.message))
        }
        response
    }

    @Override
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void recreateAndSaveManifest() {
        manifestSource.saveManifest(manifestCreator.createManifest())
        manifestService.syncManifest()
    }

    @Override
    boolean isEnabled() {
        return repositoryDefinition.enabled
    }
}

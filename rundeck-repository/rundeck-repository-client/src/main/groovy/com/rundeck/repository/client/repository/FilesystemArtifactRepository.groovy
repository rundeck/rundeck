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

import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.manifest.FilesystemManifestCreator
import com.rundeck.repository.client.manifest.FilesystemManifestSource
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.validators.BinaryValidator
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.manifest.ManifestSource


class FilesystemArtifactRepository implements ArtifactRepository {
    private static final String MEMORY_MANIFEST_SOURCE = "memory"
    private static final String ARTIFACT_BASE = "artifacts/"
    private static final String BINARY_BASE = "binary/"
    private RepositoryDefinition repositoryDefinition
    private ManifestService manifestService
    private FilesystemManifestCreator manifestCreator
    private File repoBase
    private ManifestSource manifestSource

    FilesystemArtifactRepository(RepositoryDefinition repoDef) {
        if(!repoDef.configProperties.repositoryLocation) throw new Exception("Path to repository location must be provided by setting configProperties.repositoryLocation in repository definition.")
        if(!repoDef.configProperties.manifestLocation && !repoDef.configProperties.manifestType == MEMORY_MANIFEST_SOURCE) throw new Exception("Path to manifest must be provided by setting configProperties.manifestLocation in repository definition.")
        this.repositoryDefinition = repoDef
        repoBase = new File(repoDef.configProperties.repositoryLocation)
        if(!repoBase.exists()) {
            if(!repoBase.mkdirs()) throw new Exception("Repository base dir: ${repoBase.absolutePath} does not exist. Unable to create dir")
        }

        ensureExists(repoBase,ARTIFACT_BASE)
        ensureExists(repoBase,BINARY_BASE)
        manifestSource = repoDef.configProperties.manifestType == MEMORY_MANIFEST_SOURCE ? new MemoryManifestSource() : new FilesystemManifestSource(repoDef.configProperties.manifestLocation)
        manifestService = new MemoryManifestService(manifestSource)
        manifestCreator = new FilesystemManifestCreator(repoBase.absolutePath+"/artifacts")
    }

    void ensureExists(final File base, final String dirName) {
        new File(base,dirName).mkdirs()
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
        return ArtifactUtils.createArtifactFromYamlStream(new File(repoBase, ARTIFACT_BASE+ ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)).newInputStream())
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        return new File(repoBase,BINARY_BASE+ArtifactUtils.artifactBinaryFileName(artifactId,artifactVer,extension)).newInputStream()
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        ResponseBatch rbatch = new ResponseBatch()
        try {
            File saveFile = new File(repoBase,ARTIFACT_BASE+artifact.artifactMetaFileName)
            ArtifactUtils.saveArtifactToOutputStream(artifact, saveFile.newOutputStream())
            recreateAndSaveManifest()
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.META_UPLOAD_FAILED, message:ex.message))
        }
        return rbatch
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        responseBatch.messages.addAll(BinaryValidator.validate(artifactFileset.artifact.artifactType, artifactFileset.artifactBinary).messages)
        if(!responseBatch.batchSucceeded()) return responseBatch

        responseBatch.messages.addAll(saveNewArtifact(artifactFileset.artifact).messages)
        responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)

        responseBatch
    }

    ResponseBatch uploadArtifactBinary(final RundeckRepositoryArtifact artifact, final InputStream inputStream) {
        ResponseBatch rbatch = new ResponseBatch()
        try {
            File saveFile = new File(repoBase,BINARY_BASE+artifact.artifactBinaryFileName)
            saveFile << inputStream
            rbatch.addMessage(ResponseMessage.success())
        } catch(Exception ex) {
            rbatch.addMessage(new ResponseMessage(code: ResponseCodes.META_UPLOAD_FAILED,message:ex.message))
        }
        return rbatch
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

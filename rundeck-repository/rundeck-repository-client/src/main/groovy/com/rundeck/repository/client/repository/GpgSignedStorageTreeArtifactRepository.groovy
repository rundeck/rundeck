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
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.signing.GpgPassphraseProvider
import com.rundeck.repository.client.signing.GpgTools
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import com.rundeck.repository.client.validators.BinaryValidator
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.events.RepositoryEventEmitter
import com.rundeck.repository.manifest.ManifestEntry
import org.rundeck.storage.data.DataUtil


class GpgSignedStorageTreeArtifactRepository extends StorageTreeArtifactRepository {
    private static final String SIG_SUFFIX = ".sig"
    File gpgPublicKey
    File gpgPrivateKey

    GpgPassphraseProvider passphraseProvider
    static final ResourceFactory resourceFactory = new ResourceFactory()

    GpgSignedStorageTreeArtifactRepository(
            final File gpgPublicKey,
            final File gpgPrivateKey,
            final GpgPassphraseProvider passphraseProvider,
            final StorageTree storageTree,
            final RepositoryDefinition repositoryDefinition,
            final RepositoryEventEmitter eventEmitter
    ) {
        super(storageTree, repositoryDefinition, eventEmitter)
        this.gpgPublicKey = gpgPublicKey
        this.gpgPrivateKey = gpgPrivateKey
        this.passphraseProvider = passphraseProvider
    }

    GpgSignedStorageTreeArtifactRepository(
            final File gpgPublicKey,
            final File gpgPrivateKey,
            final GpgPassphraseProvider passphraseProvider,
            final StorageTree storageTree,
            final RepositoryDefinition repositoryDefinition
    ) {
        this(gpgPublicKey, gpgPrivateKey, passphraseProvider, storageTree, repositoryDefinition, null)
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version = null) {
       def entry = manifestService.getEntry(artifactId)
       if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
       String artifactVer = version ?: entry.currentVersion
       String metaPath = ArtifactUtils.artifactMetaFileName(artifactId, artifactVer)
       InputStream artifactFile = storageTree.getResource(artifactBase+ metaPath).contents.inputStream
       InputStream artifactSig = storageTree.getResource(artifactBase+ metaPath+ SIG_SUFFIX).contents.inputStream
       GpgTools.validateSignature(artifactFile, artifactSig, gpgPublicKey.newInputStream())
       return super.getArtifact(artifactId)
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
        String artifactVer = version ?: entry.currentVersion
        String extension = ArtifactUtils.artifactTypeFromNice(entry.artifactType).extension
        String binaryPath = ArtifactUtils.artifactBinaryFileName(artifactId,artifactVer,extension)
        InputStream artifactFile = storageTree.getResource(artifactBase+ binaryPath).contents.inputStream
        InputStream artifactSig = storageTree.getResource(artifactBase+ binaryPath+ SIG_SUFFIX).contents.inputStream
        GpgTools.validateSignature(artifactFile, artifactSig, gpgPublicKey.newInputStream())
        return super.getArtifactBinary(artifactId)
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        if(!gpgPrivateKey) return super.uploadArtifact(artifactInputStream)

        ResponseBatch responseBatch = new ResponseBatch()
        ArtifactFileset artifactFileset = ArtifactUtils.constructArtifactFileset(artifactInputStream)
        responseBatch.messages.addAll(BinaryValidator.validate(artifactFileset.artifact.artifactType, artifactFileset.artifactBinary).messages)
        if(!responseBatch.batchSucceeded()) return responseBatch


        responseBatch.messages.addAll(saveNewArtifact(artifactFileset.artifact).messages)

        File binarySig = File.createTempFile("binary","sig")
        GpgTools.signDetached(false,gpgPrivateKey.newInputStream(),artifactFileset.artifactBinary.newInputStream(),binarySig.newOutputStream(),passphraseProvider)
        String sigPath = binaryBase+ artifactFileset.artifact.getArtifactBinaryFileName()+ ".sig"
        def sigResource = DataUtil.withStream(binarySig.newInputStream(), [:], resourceFactory)
        storageTree.createResource(sigPath,sigResource)
        responseBatch.messages.addAll(uploadArtifactBinary(artifactFileset.artifact, artifactFileset.artifactBinary.newInputStream()).messages)
        responseBatch
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        ResponseBatch batch = new ResponseBatch()
        try {
        if(gpgPrivateKey) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream()
            ArtifactUtils.saveArtifactToOutputStream(artifact,bout)
            File metaSig = File.createTempFile("meta","sig")
            GpgTools.signDetached(false,gpgPrivateKey.newInputStream(),new ByteArrayInputStream(bout.toByteArray()),metaSig.newOutputStream(),passphraseProvider)
            String sigPath = artifactBase+ artifact.getArtifactMetaFileName()+ ".sig"
            def sigResource = DataUtil.withStream(metaSig.newInputStream(), [:], resourceFactory)
            storageTree.createResource(sigPath,sigResource)
        }
        } catch(Exception ex) {
          batch.messages.add(new ResponseMessage(code: ResponseCodes.ARTIFACT_SIGNING_FAILED, message: ex.message))
        }
        batch.messages.addAll(super.saveNewArtifact(artifact).messages)
        return batch
    }

}

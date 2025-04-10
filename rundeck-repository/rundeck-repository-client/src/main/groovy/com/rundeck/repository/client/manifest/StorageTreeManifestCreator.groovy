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
package com.rundeck.repository.client.manifest

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.repository.StorageTreeArtifactRepository
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.PathUtils
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry
import org.rundeck.storage.api.Tree

class StorageTreeManifestCreator extends AbstractManifestCreator {

    private final Tree<ResourceMeta> repoTree
    private final String artifactPath

    StorageTreeManifestCreator(Tree<ResourceMeta> repoTree, String treePath) {
        this.artifactPath = PathUtils.composePath(treePath, StorageTreeArtifactRepository.ARTIFACT_BASE)
        this.repoTree = repoTree
    }

    @Override
    ArtifactManifest createManifest() {
        ArtifactManifest manifest = new ArtifactManifest()
        repoTree.listDirectoryResources(artifactPath).each { resource ->
            RundeckRepositoryArtifact artifact = ArtifactUtils.createArtifactFromYamlStream(resource.contents.inputStream)
            ManifestEntry entry = artifact.createManifestEntry()
            entry.lastRelease = resource.contents.creationTime.time
            addEntryToManifest(manifest,entry)
        }

        return manifest
    }
}

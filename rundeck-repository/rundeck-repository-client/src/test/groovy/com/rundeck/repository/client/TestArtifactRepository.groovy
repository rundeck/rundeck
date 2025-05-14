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
package com.rundeck.repository.client

import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository


class TestArtifactRepository implements ArtifactRepository {
    MemoryManifestService service = new MemoryManifestService(new MemoryManifestSource())
    RepositoryDefinition repoDefn

    TestArtifactRepository(MemoryManifestService service, RepositoryDefinition repDefn) {
        this.service = service
        this.repoDefn = repDefn
        this.service.syncManifest()
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repoDefn
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version) {
        return null
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version) {
        return null
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact verbArtifact) {
        return null
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        return null
    }

    @Override
    ManifestService getManifestService() {
        return service
    }

    @Override
    void recreateAndSaveManifest() {

    }

    @Override
    boolean isEnabled() {
        return true
    }
}

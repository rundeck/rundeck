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
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.manifest.search.ManifestSearch
import com.rundeck.repository.manifest.search.ManifestSearchResult

interface RepositoryClient {

    /**
     * Developers who only want to provide a link to their binary need a way to add an artifact information
     * to the repository. Artifact data is immutable so a new file will always be saved to the repository.
     * An error will be returned to the developer if an artifact with the id and version already exists.
     *
     * @param repositoryName
     * @param artifactBinaryStream
     * @return
     */
    ResponseBatch saveNewArtifact(String repositoryName, RepositoryArtifact repositoryArtifact)
    /**
     * Upload a new artifact to a repository
     * An error will be returned to the developer if an artifact with the id and version already exists.
     *
     * @param repositoryName
     * @param artifactBinaryStream
     * @return
     */
    ResponseBatch uploadArtifact(String repositoryName, InputStream artifactBinaryStream)
    /**
     * Install the artifact to Rundeck using the configured artifact installer
     * @param artifact
     * @return
     */
    ResponseBatch installArtifact(String repositoryName, String artifactId, String version)
    Collection<ManifestSearchResult> searchManifests(ManifestSearch search)
    Collection<ManifestSearchResult> listArtifacts(Integer offset, Integer limit)
    Collection<ManifestSearchResult> listArtifactsByRepository(String repoName, Integer offset, Integer limit)
    /**
     * Get the artifact metadata from the repository
     *
     * @param repositoryName
     * @param artifactId
     * @param artifactVersion
     * @return
     */
    RepositoryArtifact getArtifact(String repositoryName, String artifactId, String artifactVersion)
    /**
     * Get the artifact binary from the repository if it exists. If a repository artifact specifies a link to the binary in the
     * metadata, that link will be used to download the binary
     * @param repositoryName
     * @param artifactId
     * @param artifactVersion
     * @return
     */
    InputStream getArtifactBinary(String repositoryName, String artifactId, String artifactVersion)
    /**
     * Trigger all installed repositories to re-pull the manifest sources and refresh the in memory lists of the artifacts
     */
    void syncInstalledManifests()
    /**
     * Trigger a manual recreation of the manifest for the given repo. This is useful if someone
     * add or removes artifacts from a repo without using the client
     * @param repositoryName
     */
    void refreshRepositoryManifest(String repositoryName)

    /**
     * List names of all repositories defined in the system
     * @return
     */
    List<RepositoryDefinition> listRepositories()
}
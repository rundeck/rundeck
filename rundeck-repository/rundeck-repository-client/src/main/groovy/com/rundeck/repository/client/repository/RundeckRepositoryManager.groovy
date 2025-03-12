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

import com.fasterxml.jackson.core.JsonEncoding
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.manifest.search.ManifestSearch
import com.rundeck.repository.manifest.search.ManifestSearchResult
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.definition.RepositoryDefinitionList
import com.rundeck.repository.api.RepositoryFactory
import com.rundeck.repository.api.RepositoryManager
import com.rundeck.repository.api.ArtifactRepository


class RundeckRepositoryManager implements RepositoryManager {

    private ObjectMapper mapper = new ObjectMapper()
    private YAMLFactory yamlFactory = new YAMLFactory()
    protected Map<String, ArtifactRepository> repositories = [:]
    protected RepositoryDefinitionList repositoryDefinitions
    private URL repositoryDefinitionDatasource
    private RepositoryFactory repositoryFactory

    RundeckRepositoryManager() {
        this(new RundeckRepositoryFactory())
    }
    RundeckRepositoryManager(RepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory
    }

    @Override
    void setRepositoryDefinitionListDatasourceUrl(final String urlToRepositoryDefinitionListDatasource) {
        this.repositoryDefinitionDatasource = new URL(urlToRepositoryDefinitionListDatasource)
        repositories.clear()
        loadRepositories()
        syncRepositories()
    }

    void loadRepositories() {
        repositoryDefinitions = mapper.readValue(yamlFactory.createParser(repositoryDefinitionDatasource),RepositoryDefinitionList)
        if(!repositoryDefinitions) repositoryDefinitions = new RepositoryDefinitionList()
        repositoryDefinitions.repositories.each {
            initializeRepoFromDefinition(it)
        }
    }

    @Override
    List<RepositoryDefinition> listRepositories() {
        return repositoryDefinitions.repositories
    }

    @Override
    ResponseBatch saveNewArtifact(final String repositoryName, final RepositoryArtifact verbArtifact) {
        if(!repositories.containsKey(repositoryName)) return new ResponseBatch().withMessage(new ResponseMessage(code: ResponseCodes.REPO_DOESNT_EXIST,message:"Repository ${repositoryName} does not exist"))
        return repositories[repositoryName].saveNewArtifact(verbArtifact)
    }

    @Override
    void addRepository(final RepositoryDefinition repositoryDefinition) {
        initializeRepoFromDefinition(repositoryDefinition)
        repositoryDefinitions.repositories.add(repositoryDefinition)
        if(repositoryDefinition.enabled) syncRepository(repositoryDefinition.repositoryName)
        saveRepositoryDefinitionList()
    }

    protected void saveRepositoryDefinitionList() {
        mapper.writeValue(yamlFactory.createGenerator(new File(repositoryDefinitionDatasource.toURI()),
                                                      JsonEncoding.UTF8), repositoryDefinitions)
    }

    private void initializeRepoFromDefinition(final RepositoryDefinition repositoryDefinition) {
        repositories[repositoryDefinition.repositoryName] = repositoryFactory.createRepository(repositoryDefinition)
    }

    @Override
    void syncRepository(final String repositoryName) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        repositories[repositoryName].manifestService.syncManifest()
    }

    @Override
    void syncRepositories() {
        //TODO: do this in parallel
        enabledRepos().each { it.manifestService.syncManifest() }
    }

    @Override
    void refreshRepositoryManifest(final String repositoryName) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        repositories[repositoryName].recreateAndSaveManifest()
    }

    @Override
    void refreshRepositoryManifests() {
        enabledRepos().each { it.recreateAndSaveManifest() }
    }

    @Override
    void toggleRepositoryEnabled(final String repositoryName, final boolean enabled) {
        def repo = repositoryDefinitions.repositories.find { it.repositoryName == repositoryName }
        repo.enabled = enabled
        saveRepositoryDefinitionList()
    }

    @Override
    ResponseBatch uploadArtifact(final String repositoryName, final InputStream artifactInputStream) {
        if(!repositories.containsKey(repositoryName)) return new ResponseBatch().withMessage(new ResponseMessage(code: ResponseCodes.REPO_DOESNT_EXIST,message:"Repository ${repositoryName} does not exist"))
        return repositories[repositoryName].uploadArtifact(artifactInputStream)
    }

    @Override
    Collection<ManifestSearchResult> searchRepositories(final ManifestSearch search) {
        def results = []
        enabledRepos().each {
            ManifestSearchResult result = new ManifestSearchResult(repositoryName: it.repositoryDefinition.repositoryName)
            result.results = it.manifestService.searchArtifacts(search)
            results.add(result)
        }
        return results
    }

    private List<ArtifactRepository> enabledRepos() {
        repositories.values().findAll { it.enabled }
    }

    @Override
    ManifestSearchResult searchRepository(final String repositoryName, final ManifestSearch search) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        ManifestSearchResult result = new ManifestSearchResult(repositoryName:repositoryName)
        result.results = repositories[repositoryName].manifestService.searchArtifacts(search)
        return result
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(final Integer offset, final Integer max) {
        def results = []
        enabledRepos().each {
            ManifestSearchResult result = new ManifestSearchResult(repositoryName: it.repositoryDefinition.repositoryName)
            result.results = it.manifestService.listArtifacts(offset,max)
            results.add(result)
        }
        return results
    }

    @Override
    Collection<ManifestSearchResult> listArtifacts(String repoName, final Integer offset, final Integer max) {
        if(!repositories.containsKey(repoName)) throw new Exception("Repository ${repoName} does not exist.")
        def results = []
        ManifestSearchResult result = new ManifestSearchResult(repositoryName: repoName)
        result.results = repositories[repoName].manifestService.listArtifacts(offset,max)
        results.add(result)
        return results
    }

    @Override
    RepositoryArtifact getArtifact(final String repositoryName, final String artifactId, final String artifactVersion = null) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        return repositories[repositoryName].getArtifact(artifactId,artifactVersion)
    }

    @Override
    InputStream getArtifactBinary(final String repositoryName, final String artifactId, final String artifactVersion = null) {
        if(!repositories.containsKey(repositoryName)) throw new Exception("Repository ${repositoryName} does not exist.")
        return repositories[repositoryName].getArtifactBinary(artifactId,artifactVersion)
    }
}

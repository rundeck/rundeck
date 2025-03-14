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


import com.rundeck.repository.Constants
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.client.TestArtifactRepository
import com.rundeck.repository.client.TestUtils
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.client.manifest.search.ManifestSearchImpl
import com.rundeck.repository.client.manifest.search.StringSearchTerm
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.definition.RepositoryDefinitionList
import com.rundeck.repository.api.RepositoryOwner
import com.rundeck.repository.api.RepositoryType
import spock.lang.Specification


class RundeckRepositoryManagerTest extends Specification {
    def "setRepositoryDefinitionListDatasourceUrl"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())
        then:
        manager.listRepositories().size() == 2
        manager.listRepositories().find{ it.repositoryName == "private" }
        manager.listRepositories().find{ it.repositoryName == "official" }
    }

    def "AddRepository"() {
        setup:
        File tmpRepoDef = File.createTempFile("tmp","repodefn.yaml")
        tmpRepoDef.createNewFile()
        tmpRepoDef << "---"
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.setRepositoryDefinitionListDatasourceUrl(tmpRepoDef.toURL().toString())
        RepositoryDefinition rd = new RepositoryDefinition()
        rd.repositoryName = "MyRepo"
        rd.owner = RepositoryOwner.PRIVATE
        rd.type = RepositoryType.FILE
        rd.configProperties.repositoryLocation = "/tmp/myrepo"
        rd.configProperties.manifestLocation = "/tmp/myrepo-manifest.json"
        manager.addRepository(rd)
        then:
        manager.listRepositories().size() == 1
        manager.listRepositories().find{ it.repositoryName == "MyRepo" }
    }

    def "Upload Artifact Fail with invalid repo name"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        ResponseBatch response = manager.uploadArtifact("invalid",getClass().getClassLoader().getResourceAsStream(Constants.ARTIFACT_META_FILE_NAME))
        then:
        !response.batchSucceeded()
        response.messages[0].code == ResponseCodes.REPO_DOESNT_EXIST
    }

    def "Get artifact referencing a bad repo name should throw an exception"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.getArtifact("invalid","doesn'tmatter")
        then:
        Exception ex = thrown()
        ex.message == "Repository invalid does not exist."
    }

    def "Get artifact binary referencing a bad repo name should throw an exception"() {
        when:
        RundeckRepositoryManager manager = new RundeckRepositoryManager()
        manager.getArtifactBinary("invalid","doesn'tmatter")
        then:
        Exception ex = thrown()
        ex.message == "Repository invalid does not exist."
    }

    def "Search repositories"() {
        when:
        TestRundeckRepositoryManager manager = new TestRundeckRepositoryManager()
        def results = manager.searchRepositories(new ManifestSearchImpl(terms: [new StringSearchTerm(attributeName: "name", searchValue: "Git Plugin")]))

        then:
        results.size() == 1

    }

    def "Enable/Disable repositories"() {
        setup:
        File repoLoc = File.createTempDir()

        TestRundeckRepositoryManager manager = new TestRundeckRepositoryManager()
        RepositoryDefinition rd = new RepositoryDefinition()
        rd.repositoryName = "MyRepo"
        rd.owner = RepositoryOwner.PRIVATE
        rd.type = RepositoryType.FILE
        rd.configProperties.repositoryLocation = repoLoc.absolutePath
        rd.configProperties.manifestType = "memory"
        manager.addRepository(rd)

        expect:
        manager.listRepositories().size() == 2
        manager.listRepositories().find { it.repositoryName == "private" }.enabled
        manager.listRepositories().find { it.repositoryName == "MyRepo" }.enabled
        manager.toggleRepositoryEnabled("MyRepo",false)
        manager.listRepositories().find { it.repositoryName == "private" }.enabled
        !manager.listRepositories().find { it.repositoryName == "MyRepo" }.enabled



    }

    class TestRundeckRepositoryManager extends RundeckRepositoryManager {

        TestRundeckRepositoryManager() {
            def privateRepoDefn = new RepositoryDefinition(repositoryName: "private")
            repositories["private"] = new TestArtifactRepository(new MemoryManifestService(new MemoryManifestSource(manifest:TestUtils.createTestManifest())),privateRepoDefn)
            if(!repositoryDefinitions) repositoryDefinitions = new RepositoryDefinitionList()
            repositoryDefinitions.repositories.add(privateRepoDefn)
        }

        @Override
        protected void saveRepositoryDefinitionList() {

        }
    }

}

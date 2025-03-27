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

import com.dtolabs.rundeck.core.storage.StorageTreeImpl
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.artifact.StorageTreeArtifactInstaller
import com.rundeck.repository.client.repository.RundeckRepositoryFactory
import com.rundeck.repository.client.repository.RundeckRepositoryManager
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Shared
import spock.lang.Specification

class RundeckRepositoryClientTest extends Specification {

    @Shared
    File repoRoot
    @Shared
    File buildDir
    @Shared
    String builtNotifierPath = "notifier-0.1.0.jar"

    def setupSpec() {
        buildDir = File.createTempDir()
        println buildDir.absolutePath
        repoRoot = File.createTempDir()
        if(repoRoot.exists()) repoRoot.deleteDir()
        repoRoot.mkdirs()
        new File(repoRoot,"manifest.json") << "{}" //Init empty manifest
        TestPluginGenerator.generate("notifier-0.1.0", "jar","Notification",buildDir.absolutePath)

        TestPluginGenerator.generate("scriptit", "script","NodeExecutor",buildDir.absolutePath)

        TestPluginGenerator.generate("downloadme", "script","WorkflowNodeStep",buildDir.absolutePath)
    }

    def "Upload Artifact To Repo"() {
        when:
        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        def response = client.uploadArtifact("private",new File(buildDir,builtNotifierPath).newInputStream())
        def response2 = client.uploadArtifact("private",new File(buildDir.absolutePath+"/scriptit.zip").newInputStream())

        def yaml = TestPluginGenerator.createPluginYaml("downloadme", "WorkflowNodeStep")
        def stream = new ByteArrayInputStream(yaml.getBytes())
        def response3 = client.saveNewArtifact("private",ArtifactUtils.createArtifactFromRundeckPluginYaml(stream))

        then:
        response.batchSucceeded()
        response2.batchSucceeded()
        response3.batchSucceeded()

    }

    def "Install Artifact To Plugin Storage"() {
        setup:
        File pluginRoot = File.createTempDir()
        if(pluginRoot.exists()) pluginRoot.deleteDir()
        pluginRoot.mkdirs()

        when:
        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.artifactInstaller = new StorageTreeArtifactInstaller(new StorageTreeImpl(FileTreeUtil.forRoot(pluginRoot, new ResourceFactory())),"/")
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        RundeckRepositoryArtifact artifact = ArtifactUtils.getMetaFromUploadedFile(new File(buildDir,builtNotifierPath))
        def response = client.installArtifact("private",artifact.id)

        then:
        response.batchSucceeded()

    }

    def "List Artifacts"() {
        given:

        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifacts()

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }

    def "List Artifacts By Repo"() {
        given:

        RundeckRepositoryClient client = new RundeckRepositoryClient()
        client.repositoryManager = new RundeckRepositoryManager(new RundeckRepositoryFactory())
        client.repositoryManager.setRepositoryDefinitionListDatasourceUrl(getClass().getClassLoader().getResource("repository-definition-list.yaml").toString())

        when:
        def manifestSearchResults = client.listArtifactsByRepository("private")

        then:
        manifestSearchResults.size() == 1
        manifestSearchResults[0].results.size() == 3

    }
}

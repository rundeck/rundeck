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

import com.dtolabs.rundeck.core.storage.StorageConverterPluginAdapter
import com.dtolabs.rundeck.core.storage.StorageTimestamperConverter
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.api.RepositoryOwner
import com.rundeck.repository.api.RepositoryType
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.TestPluginGenerator
import com.rundeck.repository.client.TestUtils
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import com.rundeck.repository.definition.RepositoryDefinition
import org.rundeck.storage.conf.TreeBuilder
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Shared
import spock.lang.Specification


class StorageTreeRepositoryArtifactRepositoryTest extends Specification {

    @Shared
    String treeSubPath = "repo1"
    @Shared
    File repoBase
    @Shared
    File buildDir
    @Shared
    File buildNotifierPlugin
    @Shared
    StorageTreeArtifactRepository repo

    def setupSpec() {
        repoBase = File.createTempDir()
        def tree = FileTreeUtil.forRoot(repoBase, new ResourceFactory())
        TreeBuilder tbuilder = TreeBuilder.builder(tree)
        def timestamptree = tbuilder.convert(new StorageConverterPluginAdapter(
                "builtin:timestamp",
                new StorageTimestamperConverter()
        )).build()

        buildDir = File.createTempDir()
        println repoBase.absolutePath
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "private-storage-tree-test"
        repoDef.configProperties.manifestType = "tree"
        repoDef.configProperties.manifestPath = "manifest.json"
        repoDef.configProperties.storageTreePath = "/"+treeSubPath
        repoDef.type = RepositoryType.STORAGE_TREE
        repoDef.owner = RepositoryOwner.PRIVATE
        repo = new StorageTreeArtifactRepository(StorageUtil.asStorageTree(timestamptree), repoDef)
        repo.manifestService.syncManifest()
        buildNotifierPlugin=TestPluginGenerator.generate("Notifier", "jar", "Notification", buildDir.absolutePath,[version:'0.1.0'])

    }

    def "SaveArtifactMetaToRepo"() {
        when:
        RepositoryArtifact artifact = ArtifactUtils.createArtifactFromYamlStream(getClass().getClassLoader().getResourceAsStream("rundeck-repository-artifact.yaml"))
        ResponseBatch rbatch = repo.saveNewArtifact(artifact)

        then:
        rbatch.batchSucceeded()
        new File(repoBase,"content/${treeSubPath}/artifacts/4819d98fea70-0.1.yaml").exists()
    }

    def "UploadArtifactBinary"() {
        when:
        ResponseBatch rbatch = repo.uploadArtifact(buildNotifierPlugin.newInputStream())

        then:
        rbatch.batchSucceeded()
        new File(repoBase,"content/${treeSubPath}/artifacts/882ddccbcdd9-0.1.0.yaml").exists()
        new File(repoBase,"content/${treeSubPath}/binary/882ddccbcdd9-0.1.0.jar").exists()
    }

    def "Upload Legacy 1.2 Binary"() {
        when:
        ResponseBatch rbatch = repo.uploadArtifact(getClass().getClassLoader().getResourceAsStream("legacy-plugins/src-refresh-plugin-1.2.jar"))

        then:
        rbatch.batchSucceeded()
        new File(repoBase,"content/${treeSubPath}/artifacts/93a530685018-3.0.1-SNAPSHOT.yaml").exists()
        new File(repoBase,"content/${treeSubPath}/binary/93a530685018-3.0.1-SNAPSHOT.jar").exists()
    }

    def "GetArtifact"() {
        expect:
        repo.getArtifact("4819d98fea70")
    }

    def "GetArtifact bad artifact id throws ArtifactNotFoundException"() {
        when:
        repo.getArtifact("doesnotexist")

        then:
        thrown(ArtifactNotFoundException)
    }

    def "GetArtifactBinary"() {
        expect:
        repo.getArtifactBinary("882ddccbcdd9")
    }

}

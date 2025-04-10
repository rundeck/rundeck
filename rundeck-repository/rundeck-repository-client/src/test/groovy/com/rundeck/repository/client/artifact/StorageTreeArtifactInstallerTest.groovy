/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package com.rundeck.repository.client.artifact

import com.dtolabs.rundeck.core.storage.StorageConverterPluginAdapter
import com.dtolabs.rundeck.core.storage.StorageTimestamperConverter
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.rundeck.repository.client.TestPluginGenerator
import com.rundeck.repository.client.util.ArtifactFileset
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.ResourceFactory
import org.rundeck.storage.conf.TreeBuilder
import org.rundeck.storage.data.file.FileTreeUtil
import spock.lang.Specification

class StorageTreeArtifactInstallerTest extends Specification {
    def "InstallArtifact"() {
        when:
        File buildDir = File.createTempDir()
        File pluginBinary=TestPluginGenerator.generate("Notifier", "jar", "Notification", buildDir.absolutePath)

        String treePath = "mypath"
        File repoBase = File.createTempDir()
        def tree = FileTreeUtil.forRoot(repoBase, new ResourceFactory())
        TreeBuilder tbuilder = TreeBuilder.builder(tree)
        def timestamptree = tbuilder.convert(new StorageConverterPluginAdapter(
                "builtin:timestamp",
                new StorageTimestamperConverter()
        )).build()
        StorageTreeArtifactInstaller installer = new StorageTreeArtifactInstaller(StorageUtil.asStorageTree(timestamptree), treePath)

        ArtifactFileset artifact = ArtifactUtils.constructArtifactFileset(pluginBinary)
        installer.installArtifact(artifact.artifact,pluginBinary.newInputStream())

        then:
        new File(repoBase,"content/mypath/${artifact.artifact.installationFileName}").exists()

    }
}

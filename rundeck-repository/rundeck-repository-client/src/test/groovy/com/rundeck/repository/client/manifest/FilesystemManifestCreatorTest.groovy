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

import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.google.common.io.Files
import com.rundeck.repository.client.TestPluginGenerator
import com.rundeck.repository.client.TestUtils
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry
import spock.lang.Specification


class FilesystemManifestCreatorTest extends Specification {

    def "Create Manifests Handling multiple versions"() {
        setup:
        File tempManifestDir = File.createTempDir()
        File tempScriptDir = File.createTempDir()
        String artifactId = PluginUtils.generateShaIdFromName("Script Plugin Multiver")
        File built=TestPluginGenerator.generate("Script Plugin Multiver", "script", "NodeExecutor", tempScriptDir.absolutePath)

        Files.move(built,new File(tempManifestDir,"${artifactId}-1.0.0.zip"))
        Thread.sleep(1000)
        File built2=TestPluginGenerator.generate("Script Plugin Multiver", "script", "NodeExecutor", tempScriptDir.absolutePath,[version:'1.1'])
        Files.move(built2,new File(tempManifestDir,"${artifactId}-1.1.zip"))
        File otherfile=TestPluginGenerator.generate("Other Artifact", "script", "WorkflowNodeStep", tempScriptDir.absolutePath)
        Files.move(otherfile,new File(tempManifestDir,otherfile.name))


        when:
        FilesystemManifestCreator creator = new FilesystemManifestCreator(tempManifestDir.absolutePath)
        ArtifactManifest manifest = creator.createManifest()
        ManifestEntry multiVer = manifest.entries.find { it.id == artifactId}

        then:
        manifest.entries.size() == 2
        multiVer.currentVersion == "1.1"
        multiVer.oldVersions == ["1.0.0"]


    }


}

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
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FilesystemManifestCreator extends AbstractManifestCreator {
    private static final Logger logger = LoggerFactory.getLogger(this)

    private final String artifactScanDir

    FilesystemManifestCreator(String artifactScanDir) {
        this.artifactScanDir = artifactScanDir
    }

    @Override
    ArtifactManifest createManifest() {
        File scanDir = new File(artifactScanDir)
        if(!scanDir.exists() || !scanDir.isDirectory()) {
            throw new FileNotFoundException("${artifactScanDir} does not exists or is not a directory.")
        }
        ArtifactManifest manifest = new ArtifactManifest()
        scanDir.traverse(type: groovy.io.FileType.FILES,nameFilter: ~/.*[\.jar|\.zip|\.yaml]$/) { file ->
            RundeckRepositoryArtifact artifact = null
            try {
                if (file.name.endsWith(".yaml")) {
                    try {
                        artifact = ArtifactUtils.createArtifactFromYamlStream(file.newInputStream())
                    } catch (Exception ex) {
                        ex.printStackTrace()
                        artifact = ArtifactUtils.createArtifactFromRundeckPluginYaml(file.newInputStream())
                    }
                    if(!artifact.id) artifact.id = PluginUtils.generateShaIdFromName(artifact.name)
                    if(!artifact.artifactType) artifact.artifactType = ArtifactType.SCRIPT_PLUGIN
                } else {
                    artifact = ArtifactUtils.getMetaFromUploadedFile(file)
                }
                ManifestEntry entry = artifact.createManifestEntry()
                entry.lastRelease = file.lastModified()
                addEntryToManifest(manifest, entry)
            } catch(Exception ex) {
                logger.error("Unable to add entry for file ${file.absolutePath}",ex)
            }
        }
        return manifest
    }

    private ArtifactType guessArtifactType(final File file) {
        if(file.name.endsWith("jar")) return ArtifactType.JAVA_PLUGIN
        if(file.name.endsWith("zip")) return ArtifactType.SCRIPT_PLUGIN
        return ArtifactType.SCRIPT_PLUGIN
    }
}

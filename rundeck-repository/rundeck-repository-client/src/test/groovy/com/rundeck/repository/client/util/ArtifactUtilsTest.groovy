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
package com.rundeck.repository.client.util

import com.dtolabs.rundeck.core.plugins.JarPluginProviderLoader
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.client.TestPluginGenerator
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

class ArtifactUtilsTest extends Specification {


    def "get meta from uploaded file"() {
        setup:
        File buildDir = File.createTempDir()

        File jarplugin=TestPluginGenerator.generate("MyJavaPlugin", "java","Notification",buildDir.absolutePath)
        File myscriptplugin=TestPluginGenerator.generate("MyScriptPlugin", "script","FileCopier",buildDir.absolutePath)
        File manualzipplugin=TestPluginGenerator.generate("ManualZipScriptPlugin", "script","FileCopier",buildDir.absolutePath)


        when:
        def jarMeta = ArtifactUtils.getMetaFromUploadedFile(jarplugin)
        def scriptMeta = ArtifactUtils.getMetaFromUploadedFile(myscriptplugin)
        def manualZipScriptMeta = ArtifactUtils.getMetaFromUploadedFile(manualzipplugin)

        then:
        jarMeta.name == "MyJavaPlugin"
        jarMeta.artifactType == ArtifactType.JAVA_PLUGIN
        scriptMeta.name == "MyScriptPlugin"
        scriptMeta.artifactType == ArtifactType.SCRIPT_PLUGIN
        scriptMeta.originalFilename == "MyScriptPlugin.zip"
        manualZipScriptMeta.name == "ManualZipScriptPlugin"
        manualZipScriptMeta.artifactType == ArtifactType.SCRIPT_PLUGIN
        manualZipScriptMeta.originalFilename == "ManualZipScriptPlugin.zip"

    }

    def "rename script"() {
        setup:
        File buildDir = File.createTempDir()
        TestPluginGenerator.generate("MyScriptPlugin", "script","FileCopier",buildDir.absolutePath)
        TestPluginGenerator.generate("ManualZipScriptPlugin", "script","FileCopier",buildDir.absolutePath)
//        TestUtils.gradlePluginZip(new File(buildDir,"myscriptplugin"))
//        TestUtils.zipDir(new File(buildDir,"manualzipscriptplugin").absolutePath)
        File destFile = File.createTempFile("tmp","script")
        Files.copy(new File(buildDir,"MyScriptPlugin.zip").toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        File destFile2 = File.createTempFile("tmp2","script")
        Files.copy(new File(buildDir,"ManualZipScriptPlugin.zip").toPath(), destFile2.toPath(), StandardCopyOption.REPLACE_EXISTING)

        expect:
        ArtifactUtils.renameScriptFile(destFile).name == "MyScriptPlugin.zip"
        ArtifactUtils.renameScriptFile(destFile2).name == "ManualZipScriptPlugin.zip"

    }

    def "get meta from uploaded legacy 1.2 plugin"() {
        when:
        def meta = ArtifactUtils.getMetaFromUploadedFile(new File(getClass().getClassLoader().getResource("legacy-plugins/src-refresh-plugin-1.2.jar").toURI()))
        then:
        meta.id == PluginUtils.generateShaIdFromName(meta.name)
        meta.name == "Node Refresh Plugin"
        meta.version == "3.0.1-SNAPSHOT"
    }

    def "get meta from plugin with no name or version specified"() {
        when:
        File jarFile = new File(getClass().getClassLoader().getResource("legacy-plugins/irc-notification-1.0.0.jar").toURI())
        JarFile jar = new JarFile(jarFile)
        def meta = ArtifactUtils.getMetaFromUploadedFile(jarFile)
        then:
        meta.id == PluginUtils.generateShaIdFromName(jar.getManifest().getMainAttributes().getValue(JarPluginProviderLoader.RUNDECK_PLUGIN_CLASSNAMES))
        meta.name == "irc-notification-1.0.0.jar"
        meta.originalFilename == "irc-notification-1.0.0.jar"
        meta.version == "1.0.0"
    }
}

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
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.rundeck.repository.Constants
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.artifact.SupportLevel
import com.rundeck.repository.client.artifact.RundeckRepositoryArtifact
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.search.ManifestSearch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


class ArtifactUtils {
    private static Logger LOG = LoggerFactory.getLogger(ArtifactUtils)
    private static ObjectMapper mapper = new ObjectMapper()
    private static File unusedCacheDir = File.createTempDir()
    static  {
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    static ArtifactFileset constructArtifactFileset(InputStream artifactStream) {
        File uploadTmp = File.createTempFile("tmp","artifact")
        uploadTmp << artifactStream
        return constructArtifactFileset(uploadTmp)
    }

    static ArtifactFileset constructArtifactFileset(File artifactFile) {
        ArtifactFileset fileset = new ArtifactFileset()
        fileset.artifact = getMetaFromUploadedFile(artifactFile)
        fileset.artifactBinary = artifactFile
        fileset
    }

    static RundeckRepositoryArtifact getMetaFromUploadedFile(final File artifactFile) {
        if(!artifactFile.exists()) throw new Exception("Artifact file: ${artifactFile.absolutePath} does not exist!")
        RundeckRepositoryArtifact artifact = null
        ZipFile zipArtifact = new ZipFile(artifactFile)
        boolean scriptPlugin = pluginYamlExists(zipArtifact)
        if(!scriptPlugin) {
            try {
                JarPluginProviderLoader jarLoader = new JarPluginProviderLoader(
                        artifactFile,
                        unusedCacheDir,
                        unusedCacheDir
                )
                artifact = createArtifactFromPluginMetadata(jarLoader)
                artifact.providesServices = [] as Set
                jarLoader.listProviders().each {
                    artifact.providesServices.add(it.service)
                }
                if(artifact.name.endsWith(".jar")) {
                    LOG.warn("Jar artifact: ${artifact.name}. Please make sure you have specified a 'Rundeck-Plugin-Name' attribute in the jar manifest.")
                    //No plugin name was specified in the jar manifest
                    //so we need to preserve the original filename for install/uninstall to work right
                    //This happens on legacy plugins.
                    artifact.originalFilename = artifact.name
                }
                return artifact
            } catch (Exception ex) {
                //not a jar try script
            }
        }

        artifact = createArtifactFromRundeckPluginYaml(extractArtifactMetaFromZip(zipArtifact))
        artifact.originalFilename = zipArtifact.entries().find { it.name.endsWith("plugin.yaml")}.name.split("/").first()+".zip" //script plugins are strict about the filename matching the internal structure
        return artifact
    }

    static boolean pluginYamlExists(final ZipFile zipArtifactFile) {
        zipArtifactFile.entries().find { it.name.endsWith("plugin.yaml")} //If plugin.yaml exists it's more likely the artifact is a script plugin
    }

    static File renameScriptFile(final File scriptFile) {
        ZipFile zip = new ZipFile(scriptFile)
        ZipEntry root = zip.entries().find { it.name.endsWith("plugin.yaml")}
        String rootName = root.name.split("/").first()
        File destFile = new File(scriptFile.parentFile,rootName+".zip")
        Files.copy(scriptFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return destFile
    }

    static InputStream extractArtifactMetaFromZip(final ZipFile artifactZip) {
        ZipEntry emeta = artifactZip.entries().find { it.name.endsWith("plugin.yaml")}
        artifactZip.getInputStream(emeta)
    }

    static RundeckRepositoryArtifact createArtifactFromPluginMetadata(PluginMetadata pluginMetadata) {
        RundeckRepositoryArtifact artifact = new RundeckRepositoryArtifact()
        artifact.id = pluginMetadata.getPluginId()
        artifact.name = pluginMetadata.getPluginName()
        artifact.description = pluginMetadata.getPluginDescription()
        artifact.artifactType = pluginMetadata.getPluginType() == "jar" ? ArtifactType.JAVA_PLUGIN : ArtifactType.SCRIPT_PLUGIN
        artifact.version = pluginMetadata.getPluginFileVersion()
        artifact.author = pluginMetadata.getPluginAuthor()
        artifact.releaseDate = pluginMetadata.getPluginDate()?.time ?: pluginMetadata.getDateLoaded()?.time
        artifact.rundeckCompatibility = pluginMetadata.getRundeckCompatibilityVersion()
        artifact.sourceLink = pluginMetadata.getPluginSourceLink()
        artifact.docsLink = pluginMetadata.getPluginDocsLink()
        artifact.tags = pluginMetadata.getTags()
        artifact.license = pluginMetadata.getPluginLicense()
        artifact.thirdPartyDependencies = pluginMetadata.pluginThirdPartyDependencies
        artifact.targetHostCompatibility = pluginMetadata.targetHostCompatibility
        return artifact
    }

    static RundeckRepositoryArtifact createArtifactFromRundeckPluginYaml(InputStream pluginYamlStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(pluginYamlStream)
        def meta = mapper.readValue(parser, PluginMeta)
        def plugin = new PluginMetaToPluginMetadataAdaptor(meta)
        def artifact = createArtifactFromPluginMetadata(plugin)
        artifact.providesServices = [] as Set
        plugin.pluginDefs().each {
            artifact.providesServices.add(it.service)
        }
        return artifact
    }

    static RundeckRepositoryArtifact createArtifactFromYamlStream(InputStream artifactMetaStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLParser parser = yamlFactory.createParser(artifactMetaStream)
        mapper.readValue(parser, RundeckRepositoryArtifact)
    }

    static RundeckRepositoryArtifact createArtifactFromJsonStream(InputStream artifactMetaStream) {
        mapper.readValue(artifactMetaStream, RundeckRepositoryArtifact)
    }

    static def saveArtifactToOutputStream(final RepositoryArtifact verbArtifact, final OutputStream targetStream) {
        YAMLFactory yamlFactory = new YAMLFactory()
        YAMLGenerator generator = yamlFactory.createGenerator(targetStream)
        mapper.writeValue(generator,verbArtifact)
    }

    static String artifactToJson(final RepositoryArtifact verbArtifact) {
        mapper.writeValueAsString(verbArtifact)
    }

    static void writeArtifactManifestToFile(ArtifactManifest manifest, OutputStream outputStream) {
        mapper.writeValue(outputStream,manifest)
    }

    static String artifactManifestToJson(ArtifactManifest manifest) {
        mapper.writeValueAsString(manifest)
    }

    static ArtifactManifest artifactManifestFromJson(String manifestJson) {
        mapper.readValue(manifestJson, ArtifactManifest)
    }

    static ManifestEntry createManifestEntryFromInputStream(InputStream inStream) {
        mapper.readValue(inStream, ManifestEntry)
    }

    static Collection<ManifestEntry> createManifestEntryCollectionFromInputStream(InputStream inStream) {
        TypeReference ref = new TypeReference<Collection<ManifestEntry>>() {}
        mapper.readValue(inStream, ref)
    }

    static String manifestSearchToJson(ManifestSearch search) {
        mapper.writeValueAsString(search)
    }

    static String niceArtifactTypeName(ArtifactType type) {
        type.name().toLowerCase().replace("_","-")
    }

    static ArtifactType artifactTypeFromNice(String niceArtifactTypeName) {
        ArtifactType.valueOf(niceArtifactTypeName.replace("-","_").toUpperCase())
    }

    static String niceSupportLevelName(SupportLevel level) {
        if(!level) return level
        level.name().toLowerCase().replace("_"," ")
    }

    static SupportLevel supportLevelFromNice(String niceSupportLevelName) {
        SupportLevel.valueOf(niceSupportLevelName.toUpperCase().replace(" ","_"))
    }

    static String artifactMetaFileName(final String artifactId, final String artifactVersion) {
        return artifactId+"-"+artifactVersion+".yaml"
    }

    static String artifactBinaryFileName(final String artifactId, final String artifactVersion, final String artifactExtension) {
        return artifactId+"-"+artifactVersion+"."+artifactExtension
    }

    static String sanitizedPluginName(final String artifactName) {
        return artifactName.replace(" ", "-").replaceAll("[^a-zA-Z\\-]","").toLowerCase()
    }
}

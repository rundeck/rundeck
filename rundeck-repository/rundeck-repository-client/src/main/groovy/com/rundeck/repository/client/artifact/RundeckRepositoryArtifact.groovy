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
package com.rundeck.repository.client.artifact

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.artifact.SupportLevel
import com.rundeck.repository.client.util.ArtifactTypeDeserializer
import com.rundeck.repository.client.util.ArtifactTypeSerializer
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.client.util.SupportLevelTypeDeserializer
import com.rundeck.repository.client.util.SupportLevelTypeSerializer
import com.rundeck.repository.manifest.ManifestEntry
import groovy.transform.ToString

@ToString
class RundeckRepositoryArtifact implements RepositoryArtifact {

    String id
    String name
    String description
    String organization
    Long releaseDate
    @JsonSerialize(using= ArtifactTypeSerializer)
    @JsonDeserialize(using= ArtifactTypeDeserializer)
    ArtifactType artifactType
    String author
    String authorId
    String version
    String rundeckCompatibility
    String targetHostCompatibility
    @JsonSerialize(using= SupportLevelTypeSerializer)
    @JsonDeserialize(using= SupportLevelTypeDeserializer)
    SupportLevel support
    String license
    Collection<String> tags
    Collection<String> providesServices
    String thirdPartyDependencies
    String sourceLink
    String docsLink
    String binaryLink
    String originalFilename

    ManifestEntry createManifestEntry() {
        ManifestEntry entry = new ManifestEntry()
        entry.id = id
        entry.name = name
        entry.author = author
        entry.description = description
        entry.organization = organization
        entry.support = ArtifactUtils.niceSupportLevelName(support)
        entry.artifactType = ArtifactUtils.niceArtifactTypeName(artifactType)
        entry.currentVersion = version
        entry.rundeckCompatibility = rundeckCompatibility
        entry.targetHostCompatibility = targetHostCompatibility
        entry.providesServices = providesServices
        entry.tags = tags
        entry.lastRelease = releaseDate
        entry.binaryLink = binaryLink
        entry.sourceLink = sourceLink
        entry.docsLink = docsLink
        return entry
    }

    @Override
    boolean validate() {
        if(!id) throw new ArtifactValidationException("Id is not set.")
        if(!name) throw new ArtifactValidationException("Name is required.")
        if(!artifactType) throw new ArtifactValidationException("Artifact type is required.")
        if(!rundeckCompatibility) throw new ArtifactValidationException("Rundeck compatibility version is required.")
    }

    @Override
    @JsonIgnore
    String getInstallationFileName() {
        return originalFilename ? originalFilename : ArtifactUtils.sanitizedPluginName(name).toLowerCase()+ "."+ artifactType.extension
    }

    @Override
    @JsonIgnore
    String getArtifactMetaFileName() {
        return ArtifactUtils.artifactMetaFileName(id,version)
    }

    @Override
    @JsonIgnore
    String getArtifactBinaryFileName() {
        return ArtifactUtils.artifactBinaryFileName(id,version,artifactType.extension)
    }

    boolean equals(final o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        final RundeckRepositoryArtifact that = (RundeckRepositoryArtifact) o

        if (id != that.id) {
            return false
        }

        return true
    }

    int hashCode() {
        return (id != null ? id.hashCode() : 0)
    }
}

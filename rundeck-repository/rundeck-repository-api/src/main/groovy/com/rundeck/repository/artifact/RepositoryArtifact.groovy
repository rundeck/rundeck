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
package com.rundeck.repository.artifact


interface RepositoryArtifact {
    String getId()
    void setId(String id)
    String getName()
    void setName(String name)
    String getDescription()
    void setDescription(String description)
    String getOrganization()
    void setOrganization(String organization)
    Long getReleaseDate()
    void setReleaseDate(Long releaseDate)
    ArtifactType getArtifactType()
    void setArtifactType(final ArtifactType type)
    String getAuthor()
    void setAuthor(final String author)
    String getAuthorId()
    void setAuthorId(final String authorId)
    String getVersion()
    void setVersion(final String version)
    String getRundeckCompatibility()
    void setRundeckCompatibility(final String rundeckCompatibility)
    String getTargetHostCompatibility()
    void setTargetHostCompatibility(final String targetHostCompatibility)
    SupportLevel getSupport()
    void setSupport(final SupportLevel support)
    String getLicense()
    void setLicense(final String license)
    Collection<String> getTags()
    void setTags(final Collection<String> tags)
    Collection<String> getProvidesServices()
    void setProvidesServices(final Collection<String> providesServices)
    String getThirdPartyDependencies()
    void setThirdPartyDependencies(final String thirdPartyDependencies)
    String getSourceLink()
    void setSourceLink(final String sourceLink)
    String getDocsLink()
    void setDocsLink(final String docsLink)
    String getBinaryLink()
    void setBinaryLink(final String binaryLink)
    String getOriginalFilename()
    void setOriginalFilename(final String originalFilename)

    boolean validate()

    String getInstallationFileName()
    String getArtifactMetaFileName()
    String getArtifactBinaryFileName()
}
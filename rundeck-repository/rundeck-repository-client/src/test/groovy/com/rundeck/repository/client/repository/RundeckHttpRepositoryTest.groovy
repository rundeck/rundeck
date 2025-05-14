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

import com.rundeck.repository.client.RundeckRepositoryClient
import com.rundeck.repository.client.TestUtils
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.manifest.MemoryManifestService
import com.rundeck.repository.client.manifest.MemoryManifestSource
import com.rundeck.repository.client.manifest.RundeckOfficialManifestService
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification

import java.security.Security
import java.util.concurrent.TimeUnit


class RundeckHttpRepositoryTest extends Specification {
    def setup() {
        Security.addProvider(new BouncyCastleProvider());
    }

    def "Ensure Public Key Caching"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody(getClass().getClassLoader().getResourceAsStream("gpg/pubkey.key").text))
        String endpoint = httpServer.url("repo/v1/oss").toString()

        when:
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "OSS"
        RundeckHttpRepository repo = new RundeckHttpRepository(repoDef)
        repo.rundeckRepositoryEndpoint = endpoint
        repo.manifestService = new RundeckOfficialManifestService(endpoint, 1, TimeUnit.HOURS)
        def pubKey = repo.getRundeckPublicKey()
        RecordedRequest r = httpServer.takeRequest()
        def pubKeyFromCache = repo.getRundeckPublicKey()

        then:
        r.path == "/repo/v1/oss/verification-key"
        pubKey
        pubKeyFromCache
        pubKey != pubKeyFromCache
        pubKey.text == pubKeyFromCache.text

    }

    def "Attempt to download but no binary url"() {
        setup:
        ManifestEntry abcPlugin = TestUtils.createEntry("ABCPlugin")
        String artifactId = abcPlugin.id
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("{\"error\":\"Objects not found\"}"))
        String endpoint = httpServer.url("").toString()

        when:
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "OSS"
        repoDef.configProperties.rundeckRepoEndpoint = endpoint
        MemoryManifestSource msource = new MemoryManifestSource()
        msource.manifest.entries.add(abcPlugin)
        RundeckHttpRepository repo = new RundeckHttpRepository(repoDef)
        repo.manifestService = new MemoryManifestService(msource)
        repo.manifestService.syncManifest()
        def binStream = repo.getArtifactBinary(artifactId)

        then:
        Exception ex = thrown()
        ex.message == "Binary not found"

    }

    def "GetArtifact bad artifact id throws ArtifactNotFoundException"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(404))
        String endpoint = httpServer.url("repo/v1/oss").toString()

        when:
        RepositoryDefinition repoDef = new RepositoryDefinition()
        repoDef.repositoryName = "OSS"
        RundeckHttpRepository repo = new RundeckHttpRepository(repoDef)
        repo.rundeckRepositoryEndpoint = endpoint
        repo.manifestService = new RundeckOfficialManifestService(endpoint, 1, TimeUnit.HOURS)
        repo.getArtifact("doesnotexist")

        then:
        thrown(ArtifactNotFoundException)
    }

}

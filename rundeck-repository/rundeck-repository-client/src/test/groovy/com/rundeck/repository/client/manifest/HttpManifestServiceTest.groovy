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

import com.fasterxml.jackson.databind.ObjectMapper
import com.rundeck.repository.client.TestUtils
import com.rundeck.repository.client.manifest.search.CollectionContainsValueMatchChecker
import com.rundeck.repository.client.manifest.search.CollectionSearchTerm
import com.rundeck.repository.client.manifest.search.EqualsMatchChecker
import com.rundeck.repository.client.manifest.search.ManifestSearchImpl
import com.rundeck.repository.client.manifest.search.StringSearchTerm
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import spock.lang.Shared
import spock.lang.Specification


class HttpManifestServiceTest extends Specification {
    ObjectMapper mapper = new ObjectMapper()

    @Shared
    ArtifactManifest manifest = TestUtils.createTestManifest()

    def "GetEntry"() {
        setup:
        ManifestEntry testEntry = manifest.entries[0]
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(testEntry)))
        String endpoint = httpServer.url("").toString()
        endpoint = endpoint.substring(0,endpoint.length() - 1) //remove trailing slash

        when:
        def result = new HttpManifestService(endpoint).getEntry(testEntry.id)
        RecordedRequest r = httpServer.takeRequest()

        then:
        testEntry == result
        r.path == "/entry/${testEntry.id}".toString()
    }

    def "ListArtifacts"() {
        setup:
        Collection<ManifestEntry> listResults = manifest.entries.subList(0,4)
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(listResults)))
        String endpoint = httpServer.url("").toString()
        endpoint = endpoint.substring(0,endpoint.length() - 1) //remove trailing slash

        when:
        def entries = new HttpManifestService(endpoint).listArtifacts(0,4)
        RecordedRequest r = httpServer.takeRequest()

        then:
        entries.size() == 4
        r.path == "/list?offset=0&max=4"
        listResults == entries
    }

    def "SearchArtifacts"() {
        setup:
        Collection<ManifestEntry> searchResults = [manifest.entries[0],manifest.entries[2],manifest.entries[3]]
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody(mapper.writeValueAsString(searchResults)))
        String endpoint = httpServer.url("").toString()
        endpoint = endpoint.substring(0,endpoint.length() - 1) //remove trailing slash

        when:
        ManifestSearchImpl search = new ManifestSearchImpl()
        search.addSearchTerms(new StringSearchTerm(attributeName: "name", "matchChecker":new EqualsMatchChecker(), searchValue: "FlowNodeScript"))
        search.addSearchTerms(new CollectionSearchTerm(attributeName: "tags", "matchChecker":new CollectionContainsValueMatchChecker(), searchValue: ["script", "rundeck"]))

        def entries = new HttpManifestService(endpoint).searchArtifacts(search)
        RecordedRequest r = httpServer.takeRequest()

        then:
        entries.size() == 3
        r.path == "/search"
        r.body.inputStream().text == mapper.writeValueAsString(search)
        searchResults == entries

    }
}

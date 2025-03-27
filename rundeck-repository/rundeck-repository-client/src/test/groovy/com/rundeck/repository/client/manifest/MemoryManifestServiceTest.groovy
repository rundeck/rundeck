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


import com.rundeck.repository.client.manifest.search.CollectionContainsValueMatchChecker
import com.rundeck.repository.client.manifest.search.CollectionSearchTerm
import com.rundeck.repository.client.manifest.search.ManifestSearchImpl
import com.rundeck.repository.client.manifest.search.StringSearchTerm
import spock.lang.Specification


class MemoryManifestServiceTest extends Specification {

    MemoryManifestService memoryManifest

    def setup() {
        memoryManifest = new MemoryManifestService(new FilesystemManifestSource(new File(getClass().getClassLoader().getResource("memory-manifest-service-test.manifest").toURI()).absolutePath))
        memoryManifest.syncManifest()
    }

    def "ListArtifacts"() {
        expect:
        memoryManifest.listArtifacts().size() == 8
    }

    def "ListArtifacts limited"() {
        when:
        def results = memoryManifest.listArtifacts(2,2)
        then:
        results.size() == 2
        results[0].name == "Git Plugin"
    }

    def "SearchArtifacts - Name Match"() {
        when:
        ManifestSearchImpl search = new ManifestSearchImpl()
        search.addSearchTerms(new StringSearchTerm(attributeName: "name", searchValue: "Git Plugin"))
        def results = memoryManifest.searchArtifacts(search)

        then:
        results.size() == 1
        results[0].name == "Git Plugin"
    }

    def "SearchArtifacts - Tag Match"() {
        when:
        ManifestSearchImpl search1 = new ManifestSearchImpl()
        search1.addSearchTerms(new CollectionSearchTerm(attributeName: "tags", searchValue: ["rundeck"], matchChecker: new CollectionContainsValueMatchChecker()))
        def results1 = memoryManifest.searchArtifacts(search1)
        ManifestSearchImpl search2 = new ManifestSearchImpl()
        search2.addSearchTerms(new CollectionSearchTerm(attributeName: "tags", searchValue: ["bash", "script"], matchChecker: new CollectionContainsValueMatchChecker()))
        def results2 = memoryManifest.searchArtifacts(search2)

        then:
        results1.size() == 3
        results1.any { it.name == "Git Plugin" }
        results1.any { it.name == "Script Plugin"}
        results1.any { it.name == "Copy File Plugin"}

        results2.size() == 3
        results2.any { it.name == "Javascript Runner"}
        results2.any { it.name == "Bash It"}
        results2.any { it.name == "Script Plugin"}
    }
}

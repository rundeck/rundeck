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
package com.rundeck.repository.client.manifest.search

import com.rundeck.repository.manifest.ManifestEntry
import spock.lang.Specification


class ManifestSearchBuilderTest extends Specification {

    ManifestSearchBuilder builder

    def setup() {
        builder = new ManifestSearchBuilder()
    }

    def "CreateSearch with single string attribute"() {
        when:
        ManifestSearchImpl search = builder.createSearch("name: Scripter")
        then:
        search.terms.size() == 1
        search.terms[0] instanceof StringSearchTerm
        search.terms[0].matchChecker instanceof EqualsMatchChecker
        search.terms[0].attributeName == "name"
        search.terms[0].searchValue == "Scripter"
    }

    def "CreateSearch with single string attribute wildcard prefix"() {
        when:
        ManifestSearchImpl search = builder.createSearch("name: *script")
        then:
        search.terms.size() == 1
        search.terms[0] instanceof StringSearchTerm
        search.terms[0].matchChecker instanceof EndsWtihMatchChecker
        search.terms[0].attributeName == "name"
        search.terms[0].searchValue == "script"
    }

    def "CreateSearch with single string attribute wildcard suffix"() {
        when:
        ManifestSearchImpl search = builder.createSearch("name: Script*")
        then:
        search.terms.size() == 1
        search.terms[0] instanceof StringSearchTerm
        search.terms[0].matchChecker instanceof StartsWithMatchChecker
        search.terms[0].attributeName == "name"
        search.terms[0].searchValue == "Script"
    }

    def "CreateSearch with single collection attribute"() {
        when:
        ManifestSearchImpl search = builder.createSearch("tags: java")
        then:
        search.terms.size() == 1
        search.terms[0] instanceof CollectionSearchTerm
        search.terms[0].attributeName == "tags"
        search.terms[0].searchValue == ["java"]
    }

    def "CreateSearch with one valid term and and an invalid field specified"() {
        when:
        ManifestSearchImpl search = builder.createSearch("tags: java, foo: bar")
        then:
        search.terms.size() == 1
        search.terms[0] instanceof CollectionSearchTerm
        search.terms[0].attributeName == "tags"
        search.terms[0].searchValue == ["java"]
        builder.msgs.size() == 1
        builder.msgs[0] == "Rejected term: ' foo: bar' because the field foo is not a valid search field. Searchable fields are ${ManifestEntry.searchableFieldList()}"
    }

    def "CreateSearch with two string attributes"() {
        when:
        ManifestSearchImpl search = builder.createSearch("name: script*, author: rundeck")
        then:
        search.terms.size() == 2
        search.terms[0] instanceof StringSearchTerm
        search.terms[0].matchChecker instanceof StartsWithMatchChecker
        search.terms[0].attributeName == "name"
        search.terms[0].searchValue == "script"
        search.terms[1] instanceof StringSearchTerm
        search.terms[1].matchChecker instanceof EqualsMatchChecker
        search.terms[1].attributeName == "author"
        search.terms[1].searchValue == "rundeck"
    }
}

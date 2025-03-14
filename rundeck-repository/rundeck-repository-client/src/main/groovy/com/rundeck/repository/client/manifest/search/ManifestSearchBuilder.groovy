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
import com.rundeck.repository.manifest.search.MatchChecker


class ManifestSearchBuilder {
    public static final String WILDCARD = "*"

    List<String> msgs = []

    ManifestSearchImpl createSearch(String searchTerm) {
        ManifestSearchImpl search = new ManifestSearchImpl()
        List<String> terms = searchTerm.split(",")
        terms.each { term ->
            String attribName = null
            try {
                List<String> termParts = term.split(":")
                attribName = termParts.first().trim()
                def fieldClass = ManifestEntry.getDeclaredField(attribName).type
                String searchVal = termParts.last().trim().replace(WILDCARD,"")
                if(fieldClass == String.class) {
                    search.addSearchTerms(new StringSearchTerm(attributeName: attribName,searchValue: searchVal,matchChecker: createStringMatchChecker(termParts.last().trim())))
                } else if(fieldClass == Collection.class) {
                    search.addSearchTerms(new CollectionSearchTerm(attributeName: attribName,searchValue: [searchVal]))
                }
            } catch(NoSuchFieldException nsfe) {
                msgs.add("Rejected term: '${term}' because the field ${attribName} is not a valid search field. Searchable fields are ${ManifestEntry.searchableFieldList()}")
            } catch(Exception ex) {
                msgs.add("Search term: '${term}' was rejected for error: ${ex.message}")
            }
        }
        return search
    }

    private static MatchChecker createStringMatchChecker(final String userVal) {
        if(userVal.startsWith(WILDCARD)) {
            return new EndsWtihMatchChecker()
        } else if(userVal.endsWith(WILDCARD)) {
            return new StartsWithMatchChecker()
        }
        return new EqualsMatchChecker()
    }
}

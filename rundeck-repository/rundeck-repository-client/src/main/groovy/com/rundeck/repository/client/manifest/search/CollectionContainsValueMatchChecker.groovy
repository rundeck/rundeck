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

import com.rundeck.repository.manifest.search.MatchChecker


class CollectionContainsValueMatchChecker implements MatchChecker<Collection<String>, Collection<String>> {

    private static final String TYPE = "contains"

    @Override
    String getType() {
        return TYPE
    }

    @Override
    boolean matches(final Collection<String> checkCollection, final Collection<String> searchValue) {
        if(!checkCollection) return false
        def lowcheck = checkCollection.findAll{ it != null }.collect { it.toLowerCase() }
        return searchValue.any { lowcheck.contains(it.toLowerCase()) }
    }
}

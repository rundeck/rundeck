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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.rundeck.repository.manifest.search.MatchChecker
import com.rundeck.repository.manifest.search.SearchTerm


class StringSearchTerm implements SearchTerm<String> {
    private static final String TERM_TYPE = "STR"
    String attributeName
    String searchValue

    MatchChecker matchChecker = new EqualsMatchChecker()

    @Override
    String getTermType() {
        return TERM_TYPE
    }
}

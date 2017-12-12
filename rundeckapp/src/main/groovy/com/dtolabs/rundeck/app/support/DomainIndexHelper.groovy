/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.app.support

/**
 * Define indexes for domain class fields within the `mapping` builder section
 * @author greg
 * @since 4/21/17
 */
class DomainIndexHelper {
    static def generate( delegate, @DelegatesTo(IndexHelperBuilder) Closure defs) {
        def fieldLists = [:]
        Closure c = defs
        c.delegate = new IndexHelperBuilder(fieldLists)
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        fieldLists.each { field, idxs ->
            delegate."$field"(index: idxs.join(','))
        }
    }
}

class IndexHelperBuilder {
    Map fieldLists

    IndexHelperBuilder(final Map fieldLists) {
        this.fieldLists = fieldLists
    }

    def index(String name, List fields) {
        fields.each { field ->
            if (!fieldLists[field]) {
                fieldLists[field] = [name]
            } else {
                fieldLists[field] << name
            }
        }
    }
}

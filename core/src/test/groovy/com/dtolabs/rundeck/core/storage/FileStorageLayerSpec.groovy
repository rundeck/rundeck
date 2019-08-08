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

package com.dtolabs.rundeck.core.storage

import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.PathUtil
import spock.lang.Specification
import spock.lang.Unroll

class FileStorageLayerSpec extends Specification {

    @Unroll
    def "#method with #ctype"() {
        given:
        def layer = new FileStorageLayer()
        def path = PathUtil.asPath('files/test')
        def meta = new ResourceMetaBuilder([:])
        meta.contentType = ctype
        def istream = Mock(HasInputStream)
        when:
        def result = layer."$method"(path, meta, istream)

        then:
        meta.meta == [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE): expect]

        where:
        method           | ctype                                 | expect
        'createResource' | 'text/plain'                          | 'text/plain'
        'updateResource' | 'application/octet-stream'            | 'application/octet-stream'
        'readResource'   | 'some/type'                           | 'some/type'
        'createResource' | null                                  | 'application/octet-stream'
        'updateResource' | null                                  | 'application/octet-stream'
        'readResource'   | null                                  | 'application/octet-stream'

    }
}

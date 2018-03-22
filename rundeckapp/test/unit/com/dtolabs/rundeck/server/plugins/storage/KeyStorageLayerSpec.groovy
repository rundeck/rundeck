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

package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import com.dtolabs.rundeck.core.storage.StorageUtil
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.PathUtil
import spock.lang.Specification
import spock.lang.Unroll

class KeyStorageLayerSpec extends Specification {

    public static
    final LinkedHashMap<String, String> EXPECT_PRIVATE_META = [(KeyStorageLayer.RUNDECK_CONTENT_MASK): KeyStorageLayer.CONTENT_MASK_TYPE_CONTENT, (KeyStorageLayer.RUNDECK_KEY_TYPE): KeyStorageLayer.KEY_TYPE_PRIVATE]
    public static
    final LinkedHashMap<String, String> EXPECT_PUBLIC_META = [(KeyStorageLayer.RUNDECK_KEY_TYPE): KeyStorageLayer.KEY_TYPE_PUBLIC]
    public static
    final LinkedHashMap<String, String> EXPECT_PASSWORD_META = [(KeyStorageLayer.RUNDECK_CONTENT_MASK): KeyStorageLayer.CONTENT_MASK_TYPE_CONTENT, (KeyStorageLayer.RUNDECK_DATA_TYPE): KeyStorageLayer.KEY_TYPE_PASSWORD]

    @Unroll
    def "#method with #ctype"() {
        given:
        def layer = new KeyStorageLayer()
        def path = PathUtil.asPath('keys/test')
        def meta = new ResourceMetaBuilder([:])
        meta.contentType = ctype
        def istream = Mock(HasInputStream)
        when:
        def result = layer."$method"(path, meta, istream)

        then:
        meta.meta == (expect + [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE): ctype])

        where:
        method           | ctype                                 | expect
        'createResource' | 'text/plain'                          | [:]
        'updateResource' | 'text/plain'                          | [:]
        'readResource'   | 'text/plain'                          | [:]
        'createResource' | KeyStorageLayer.PUBLIC_KEY_MIME_TYPE  | EXPECT_PUBLIC_META
        'createResource' | KeyStorageLayer.PASSWORD_MIME_TYPE    | EXPECT_PASSWORD_META
        'createResource' | KeyStorageLayer.PRIVATE_KEY_MIME_TYPE | EXPECT_PRIVATE_META
        'updateResource' | KeyStorageLayer.PUBLIC_KEY_MIME_TYPE  | EXPECT_PUBLIC_META
        'updateResource' | KeyStorageLayer.PASSWORD_MIME_TYPE    | EXPECT_PASSWORD_META
        'updateResource' | KeyStorageLayer.PRIVATE_KEY_MIME_TYPE | EXPECT_PRIVATE_META
        'readResource'   | KeyStorageLayer.PUBLIC_KEY_MIME_TYPE  | EXPECT_PUBLIC_META
        'readResource'   | KeyStorageLayer.PASSWORD_MIME_TYPE    | EXPECT_PASSWORD_META
        'readResource'   | KeyStorageLayer.PRIVATE_KEY_MIME_TYPE | EXPECT_PRIVATE_META

    }
}

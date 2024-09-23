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
package org.rundeck.plugin.objectstore.tree

import com.dtolabs.rundeck.core.storage.StorageUtil
import groovy.transform.CompileStatic
import io.minio.StatObjectResponse
import java.util.regex.Pattern

@CompileStatic
class ObjectStoreUtils {
    static Pattern createSubdirCheckForPath(String path) {
        if(!path) return ~/(.*?)\/.*/
        return ~/${path}\/(.*?)\/.*/
    }

    static Map<String, String> objectStatToMap(StatObjectResponse statObjectResponse) {
        Map<String, String> meta = [:]
        meta["etag"] = statObjectResponse.etag()
        // In minio v8, the header names are capitalized. In the previous versions, they are in lower case.
        Set rundeckMetaKeys = statObjectResponse.headers().names().findAll { it.toLowerCase().startsWith(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX) }
        rundeckMetaKeys.each { prefixedKey ->
            String lowerCasedPrefixedKey = prefixedKey.toLowerCase()
            meta[fixKeyName(lowerCasedPrefixedKey)] = statObjectResponse.headers().values(prefixedKey)[0]
        }
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH] = statObjectResponse.size().toString()
        if (!meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME)) {
            meta[StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME] = StorageUtil.formatDate(Date.from(statObjectResponse.lastModified().toInstant()))
        }
        if (!meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)) {
            Date lastModified = Date.from(statObjectResponse.headers().getInstant("last-modified"))
            meta[StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME] = StorageUtil.formatDate(lastModified)
        }
        return meta
    }

    private static String fixKeyName(String prefixedKey) {
        String key = prefixedKey.replaceAll(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX, "")
        return key.startsWith("rundeck") ? key.capitalize() : key
    }
}

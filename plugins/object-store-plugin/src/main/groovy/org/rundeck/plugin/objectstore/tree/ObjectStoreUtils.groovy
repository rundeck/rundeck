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
import io.minio.DateFormat
import io.minio.ObjectStat

import java.util.regex.Pattern


class ObjectStoreUtils {
    static Pattern createSubdirCheckForPath(String path) {
        if(!path) return ~/(.*?)\/.*/
        return ~/${path}\/(.*?)\/.*/
    }

    static Map<String,String> objectStatToMap(ObjectStat objectStat) {
        Map<String,String> meta = [:]
        meta["etag"] = objectStat.etag()
        Set rundeckMetaKeys = objectStat.httpHeaders().keySet().findAll { it.startsWith(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX) }
        rundeckMetaKeys.each { prefixedKey ->
            meta[fixKeyName(prefixedKey)] = objectStat.httpHeaders()[prefixedKey][0]
        }
        meta[StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH] = objectStat.length().toString()
        if(!meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME)) {
            meta[StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME] = StorageUtil.formatDate(objectStat.createdTime())
        }
        if(!meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)) {
            Date lastModified = new Date(DateFormat.HTTP_HEADER_DATE_FORMAT.parseMillis(objectStat.httpHeaders()["last-modified"][0]))
            meta[StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME] = StorageUtil.formatDate(lastModified)
        }
        return meta
    }

    private static String fixKeyName(String prefixedKey) {
        String key = prefixedKey.replaceAll(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX, "")
        return key.startsWith("rundeck") ? key.capitalize() : key
    }
}

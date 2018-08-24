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
package testhelpers

import com.dtolabs.rundeck.server.storage.objectstore.tree.ObjectStoreTree
import io.minio.MinioClient
import io.minio.errors.ErrorResponseException


class MinioTestUtils {
    static void ifNotExistAdd(MinioClient mClient, String bucket, String key, String content, Map<String,String> meta) {
        def fixedheaders = [:]
        meta.each { k, v ->
            if(!k.startsWith(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX)) {
                fixedheaders[ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX+ k] = String.valueOf(v)
            }
        }
        try {
            mClient.statObject(bucket, key)
        } catch (ErrorResponseException erex) {
            if (erex.response.code() == 404) {
                ByteArrayInputStream inStream = new ByteArrayInputStream(content.bytes)
                mClient.putObject(bucket, key, inStream, content.bytes.length, fixedheaders)
            }
        }
    }
}

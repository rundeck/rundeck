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

import io.minio.MinioClient
import io.minio.PutObjectOptions
import io.minio.errors.ErrorResponseException
import org.rundeck.plugin.objectstore.tree.ObjectStoreTree


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
                PutObjectOptions putOpts = new PutObjectOptions(content.bytes.length, -1)
                putOpts.headers = fixedheaders
                mClient.putObject(bucket, key, inStream, putOpts)
            }
        }
    }

    static MinioClient startOrConnectToContainer(MinioContainer container) {
        boolean isTestEnv = false
        try {
            String testEnvCheck = System.getenv("IS_TEST_ENV")
            if (testEnvCheck) { isTestEnv = true }
        } catch (NullPointerException e) {
            System.out.println("object-store-plugin test: Error checking IS_TEST_ENV variable. Starting Minio container...")
        }

        if (!isTestEnv) { container.start() }
        return container.client(isTestEnv)
    }
}

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
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import org.rundeck.plugin.objectstore.tree.ObjectStoreTree
import java.util.concurrent.TimeUnit


class MinioTestUtils {
    static void ifNotExistAdd(MinioClient mClient, String bucket, String key, String content, Map<String,String> meta) {
        Map<String, String> fixedheaders = [:]
        meta.each { k, v ->
            if(!k.startsWith(ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX)) {
                fixedheaders[ObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX+ k] = String.valueOf(v)
            }
        }
        try {
            StatObjectArgs statArgs = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build()
            mClient.statObject(statArgs)
        } catch (ErrorResponseException erex) {
            if (erex.response.code() == 404) {
                ByteArrayInputStream inStream = new ByteArrayInputStream(content.bytes)
                PutObjectArgs putArgs = PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .stream(inStream, content.bytes.length, -1)
                        .headers(fixedheaders)
                        .build()
                mClient.putObject(putArgs)
            }
        }
    }

    static void ensureMinioServerInitialized(MinioClient minioClient) throws Exception {
        final int MAX_RETRIES = 3
        final int RETRY_DELAY_SECONDS = 1
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                minioClient.listBuckets();
                return;
            } catch (Exception e) {
                TimeUnit.SECONDS.sleep(RETRY_DELAY_SECONDS);
                retries++;
            }
        }
        throw new Exception("MinIO server is not initialized after " + MAX_RETRIES + " retries.");
    }
}

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
package org.rundeck.plugin.objectstore.stream

import com.dtolabs.utils.Streams
import io.minio.MinioClient
import org.rundeck.storage.api.HasInputStream

class LazyAccessObjectStoreInputStream implements HasInputStream {

    private final MinioClient mClient
    private final String bucket
    private final String objectKey

    LazyAccessObjectStoreInputStream(MinioClient mClient, String bucket, String objectKey) {
        this.objectKey = objectKey
        this.bucket = bucket
        this.mClient = mClient
    }

    @Override
    InputStream getInputStream() throws IOException {
        return mClient.getObject(bucket,objectKey)
    }

    @Override
    long writeContent(final OutputStream outputStream) throws IOException {
        InputStream inStream = getInputStream()
        long copied = Streams.copyStream(inStream, outputStream)
        inStream.close()
        return copied
    }
}

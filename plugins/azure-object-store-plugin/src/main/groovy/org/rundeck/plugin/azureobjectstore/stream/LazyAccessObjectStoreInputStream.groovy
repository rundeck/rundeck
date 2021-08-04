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
package org.rundeck.plugin.azureobjectstore.stream

import com.dtolabs.utils.Streams
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import io.minio.MinioClient
import org.rundeck.storage.api.HasInputStream

class LazyAccessObjectStoreInputStream implements HasInputStream {

    private final CloudStorageAccount storageAccount
    private final String bucket
    private final String objectKey

    LazyAccessObjectStoreInputStream(CloudStorageAccount storageAccount, String bucket, String objectKey) {
        this.objectKey = objectKey
        this.bucket = bucket
        this.storageAccount = storageAccount
    }

    @Override
    InputStream getInputStream() throws IOException {
        CloudBlobClient client = storageAccount.createCloudBlobClient()
        CloudBlobContainer container = client.getContainerReference(bucket)
        OutputStream stream = null
        container.getBlockBlobReference(objectKey) //gotta get an inputstream
        InputStream inputStream
        return inputStream
    }

    @Override
    long writeContent(final OutputStream outputStream) throws IOException {
        InputStream inStream = getInputStream()
        long copied = Streams.copyStream(inStream, outputStream)
        inStream.close()
        return copied
    }
}

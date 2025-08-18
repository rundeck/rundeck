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

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import com.dtolabs.rundeck.core.storage.StorageUtil
import groovy.transform.CompileStatic
import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import org.rundeck.plugin.objectstore.directorysource.ObjectStoreDirectorySource
import org.rundeck.plugin.objectstore.directorysource.ObjectStoreMemoryDirectorySource
import org.rundeck.plugin.objectstore.stream.CloseAfterCopyStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.Tree

import java.util.regex.Pattern

@CompileStatic
class ObjectStoreTree implements Tree<BaseStreamResource> {
    public static final String RUNDECK_CUSTOM_HEADER_PREFIX = "x-amz-meta-rdk-"
    private static final String DIR_MARKER = "/"

    private final String bucket
    private final MinioClient mClient
    private final ObjectStoreDirectorySource directorySource

    ObjectStoreTree(MinioClient mClient, String bucket) {
        this(mClient,bucket,new ObjectStoreMemoryDirectorySource(mClient, bucket))
    }

    ObjectStoreTree(MinioClient mClient, String bucket, ObjectStoreDirectorySource directorySource) {
        this.mClient = mClient
        this.bucket = bucket
        this.directorySource = directorySource
        init()
    }

    private void init() {
        BucketExistsArgs args = BucketExistsArgs.builder()
                .bucket(bucket)
                .build();
        if(!mClient.bucketExists(args)) {
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build()
            mClient.makeBucket(makeBucketArgs)
        }
    }

    @Override
    boolean hasPath(final Path path) {
        return hasPath(path.path)
    }

    @Override
    boolean hasPath(final String path) {
        return directorySource.checkPathExists(path)
    }

    @Override
    boolean hasResource(final Path path) {
        return hasResource(path.path)
    }

    @Override
    boolean hasResource(final String path) {
        directorySource.checkResourceExists(path)
    }

    @Override
    boolean hasDirectory(final Path path) {
        return hasDirectory(path.path)
    }

    @Override
    boolean hasDirectory(final String path) {
        return directorySource.checkPathExistsAndIsDirectory(path)
    }

    @Override
    Resource<BaseStreamResource> getPath(final Path path) {
        return getPath(path.path)
    }

    @Override
    Resource<BaseStreamResource> getPath(final String path) {
        if(hasDirectory(path)) return new ObjectStoreResource(path+ DIR_MARKER, null, true)
        return getResource(path)
    }

    @Override
    Resource<BaseStreamResource> getResource(final Path path) {
        return getResource(path.path)
    }

    @Override
    Resource<BaseStreamResource> getResource(final String path) {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build()
        BaseStreamResource content = new BaseStreamResource(directorySource.getEntryMetadata(path),
                new CloseAfterCopyStream(mClient.getObject(args)))
        ObjectStoreResource resource = new ObjectStoreResource(path, content)
        return resource
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectoryResources(final Path path) {
        return listDirectoryResources(path.path)
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectoryResources(final String path) {
        return directorySource.listResourceEntriesAt(path)
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectory(final Path path) {
        return listDirectory(path.path)
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectory(final String path) {
        return directorySource.listEntriesAndSubDirectoriesAt(path)
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectorySubdirs(final Path path) {
        return listDirectorySubdirs(path.path)
    }

    @Override
    Set<Resource<BaseStreamResource>> listDirectorySubdirs(final String path) {
        return directorySource.listSubDirectoriesAt(path)
    }

    @Override
    boolean deleteResource(final Path path) {
        deleteResource(path.path)
    }

    @Override
    boolean deleteResource(final String path) {
        RemoveObjectArgs args = RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(path)
                .build()
        mClient.removeObject(args)
        directorySource.deleteEntry(path)
        return true
    }

    @Override
    Resource<BaseStreamResource> createResource(final Path path, final BaseStreamResource content) {
        return updateResource(path.path,content)
    }

    @Override
    Resource<BaseStreamResource> createResource(final String path, final BaseStreamResource content) {
        return updateResource(path,content)
    }

    @Override
    Resource<BaseStreamResource> updateResource(final Path path, final BaseStreamResource content) {
        return updateResource(path.path,content)
    }

    @Override
    Resource<BaseStreamResource> updateResource(final String path, final BaseStreamResource content) {
        def customHeaders = createCustomHeadersFromRundeckMeta(content.meta)

        if (content.contentLength > -1) {
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(content.inputStream, content.contentLength, -1)
                    .headers(customHeaders)
                    .build()
            mClient.putObject(putObjectArgs)
        } else {
            File tmpFile = File.createTempFile("_obj_store_tmp_", "_data_")
            tmpFile << content.inputStream
            customHeaders[RUNDECK_CUSTOM_HEADER_PREFIX + StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH] = tmpFile.size().toString()
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .stream(tmpFile.newInputStream(), tmpFile.size(), -1)
                    .headers(customHeaders)
                    .build()
            mClient.putObject(putObjectArgs)
            tmpFile.delete()
        }
        directorySource.updateEntry(path, content.meta)
        return getResource(path)
    }

    static Map<String,String> createCustomHeadersFromRundeckMeta(Map rundeckMeta) {
        Map<String,String> custom = [:]
        rundeckMeta.each { k, v ->
            custom[RUNDECK_CUSTOM_HEADER_PREFIX+k] = String.valueOf(v)
        }
        custom
    }

    static Pattern nestedSubDirCheck() {
        ~/.*\\/.*\\/.*/
    }
}

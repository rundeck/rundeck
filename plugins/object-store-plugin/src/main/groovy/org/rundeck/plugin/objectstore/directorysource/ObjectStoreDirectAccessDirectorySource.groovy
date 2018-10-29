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
package org.rundeck.plugin.objectstore.directorysource

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import io.minio.MinioClient
import io.minio.errors.ErrorResponseException
import io.minio.messages.Item
import org.rundeck.plugin.objectstore.stream.LazyAccessObjectStoreInputStream
import org.rundeck.plugin.objectstore.tree.ObjectStoreResource
import org.rundeck.plugin.objectstore.tree.ObjectStoreTree
import org.rundeck.plugin.objectstore.tree.ObjectStoreUtils
import org.rundeck.storage.api.Resource

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Uses the object client to directly access the object store to get directory information.
 * For stores with lots of objects this could be a very inefficient directory access mechanism.
 *
 * This store works best when the object store is going to be accessed by multiple cluster members
 * or the object store is regularly updated by third party tools
 */
class ObjectStoreDirectAccessDirectorySource implements ObjectStoreDirectorySource {
    private static final String DIR_MARKER = "/"
    private final String bucket
    private final MinioClient mClient

    ObjectStoreDirectAccessDirectorySource(MinioClient mClient, String bucket) {
        this.mClient = mClient
        this.bucket = bucket
    }

    @Override
    boolean checkPathExists(final String path) {
        def items = mClient.listObjects(bucket, path)
        return items.size() > 0
    }

    @Override
    boolean checkResourceExists(final String path) {
        try {
            mClient.statObject(bucket, path)
            return true
        } catch(ErrorResponseException erex) {
            if(erex.response.code() == 404) return false
        }

        return false
    }

    @Override
    boolean checkPathExistsAndIsDirectory(final String path) {
        def items = mClient.listObjects(bucket, path, false)
        if(items.size() != 1) return false
        return items[0].get().objectName().endsWith(DIR_MARKER)
    }

    @Override
    Map<String, String> getEntryMetadata(final String path) {
        return ObjectStoreUtils.objectStatToMap(mClient.statObject(bucket, path))
    }

    @Override
    Set<Resource<BaseStreamResource>> listSubDirectoriesAt(final String path) {
        def resources = []
        def subdirs = [] as Set
        String lstPath = path == "" ? null : path
        Pattern directSubDirMatch = ObjectStoreUtils.createSubdirCheckForPath(lstPath)

        mClient.listObjects(bucket, lstPath, true).each { result ->
            Matcher m = directSubDirMatch.matcher(result.get().objectName())
            if(m.matches()) {
                subdirs.add(m.group(1))
            }
        }
        subdirs.sort().each { String dirname -> resources.add(new ObjectStoreResource(path+ "/"+dirname, null, true)) }
        return resources
    }

    @Override
    Set<Resource<BaseStreamResource>> listEntriesAndSubDirectoriesAt(final String path) {
        def resources = []
        Pattern directSubDirMatch = ObjectStoreUtils.createSubdirCheckForPath(path)
        def subdirs = [] as Set

        mClient.listObjects(bucket, path, true).each { result ->
            Matcher m = directSubDirMatch.matcher(result.get().objectName())
            if(m.matches()) {
                subdirs.add(path+DIR_MARKER+m.group(1))
            } else {
                resources.add(createResourceListItemWithMetadata(result.get()))
            }
        }
        subdirs.sort().each { String dirname -> resources.add(new ObjectStoreResource(dirname, null, true)) }
        return resources
    }

    @Override
    Set<Resource<BaseStreamResource>> listResourceEntriesAt(final String path) {
        def resources = []
        String lstPath = path == "" ? null : path
        String rPath = path == "" ?: path+"/"

        mClient.listObjects(bucket, lstPath, true)
               .findAll {
            !(it.get().objectName().replaceAll(rPath,"").contains("/"))
        }.each { result ->
            resources.add(createResourceListItemWithMetadata(result.get()))
        }
        return resources
    }

    private ObjectStoreResource createResourceListItemWithMetadata(final Item item) {
        BaseStreamResource content = new BaseStreamResource(getEntryMetadata(item.objectName()), new LazyAccessObjectStoreInputStream(mClient, bucket, item.objectName()))
        return new ObjectStoreResource(item.objectName(), content)
    }

    @Override
    void updateEntry(final String fullEntryPath, final Map<String, String> meta) {
        //no-op no additional action needed
    }

    @Override
    void deleteEntry(final String fullEntryPath) {
        //no-op no additional action needed
    }

    @Override
    void resyncDirectory() {
        //no-op
    }
}

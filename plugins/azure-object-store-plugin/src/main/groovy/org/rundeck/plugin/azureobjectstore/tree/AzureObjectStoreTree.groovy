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
package org.rundeck.plugin.azureobjectstore.tree

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import org.rundeck.plugin.azureobjectstore.directorysource.AzureObjectStoreDirectorySource
import org.rundeck.plugin.azureobjectstore.stream.CloseAfterCopyStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.Tree

import java.util.regex.Pattern


class AzureObjectStoreTree implements Tree<BaseStreamResource> {
    public static final String RUNDECK_CUSTOM_HEADER_PREFIX = "x-azure-meta-"
    private static final String DIR_MARKER = "/"

    private final CloudBlobContainer container
    private final AzureObjectStoreDirectorySource directorySource

    AzureObjectStoreTree(CloudBlobContainer container, AzureObjectStoreDirectorySource directorySource) {
        this.container = container
        this.directorySource = directorySource
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
        if(hasDirectory(path)) return new AzureObjectStoreResource(path+ DIR_MARKER, null, true)
        return getResource(path)
    }

    @Override
    Resource<BaseStreamResource> getResource(final Path path) {
        return getResource(path.path)
    }

    @Override
    Resource<BaseStreamResource> getResource(final String path) {

        CloudBlockBlob blob = AzureObjectStoreUtils.getBlobFile(container, path)
        CloseAfterCopyStream closeAfterCopyStream = new CloseAfterCopyStream(blob.openInputStream())

        BaseStreamResource content = new BaseStreamResource(directorySource.getEntryMetadata(path), closeAfterCopyStream)
        AzureObjectStoreResource resource = new AzureObjectStoreResource(path, content)
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
        CloudBlockBlob blob = AzureObjectStoreUtils.getBlobFile(container, path)
        blob.delete()
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
        CloudBlockBlob blob = AzureObjectStoreUtils.getBlobFile(container, path)

        if(content.contentLength > -1) {
            blob.setMetadata(customHeaders)
            blob.upload(content.inputStream, content.contentLength)
        } else {
            File tmpFile = File.createTempFile("_obj_store_tmp_","_data_")
            tmpFile << content.inputStream
            customHeaders[AzureObjectStoreUtils.cleanAzureKeyName(RUNDECK_CUSTOM_HEADER_PREFIX+ StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH)] = tmpFile.size().toString()

            blob.setMetadata(customHeaders)
            blob.upload(tmpFile.newInputStream(), tmpFile.length())
            tmpFile.delete()
        }
        directorySource.updateEntry(path,content.meta)
        return getResource(path)
    }

    static Map<String,String> createCustomHeadersFromRundeckMeta(Map rundeckMeta) {
        Map<String,String> custom = [:]
        rundeckMeta.each { k, v ->
            custom[AzureObjectStoreUtils.cleanAzureKeyName(RUNDECK_CUSTOM_HEADER_PREFIX+k)] = String.valueOf(v)
        }
        custom
    }

    static Pattern nestedSubDirCheck() {
        ~/.*\\/.*\\/.*/
    }
}

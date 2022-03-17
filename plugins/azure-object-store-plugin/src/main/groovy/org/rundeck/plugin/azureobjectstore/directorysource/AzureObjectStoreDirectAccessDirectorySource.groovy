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
package org.rundeck.plugin.azureobjectstore.directorysource

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.ListBlobItem
import org.rundeck.plugin.azureobjectstore.stream.LazyAccessObjectStoreInputStream
import org.rundeck.plugin.azureobjectstore.tree.AzureObjectStoreResource
import org.rundeck.plugin.azureobjectstore.tree.AzureObjectStoreUtils
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
class AzureObjectStoreDirectAccessDirectorySource implements AzureObjectStoreDirectorySource {
    CloudBlobContainer container

    AzureObjectStoreDirectAccessDirectorySource(CloudBlobContainer container) {
        this.container = container
    }

    CloudBlockBlob getBlobFile(String path){
        CloudBlockBlob blob = container.getBlockBlobReference(path);
        return blob
    }

    boolean checkBlob(String path){
        try {
            CloudBlockBlob blob = getBlobFile(path)
            if(blob==null){
                return false
            }
            return blob.exists()
        } catch (Exception e) {
            throw new Exception(e.getMessage(), e)
        }
    }

    @Override
    boolean checkPathExists(final String path) {
        checkBlob(path)
    }

    @Override
    boolean checkResourceExists(final String path) {
        checkBlob(path)
    }

    @Override
    boolean checkPathExistsAndIsDirectory(final String path) {
        boolean directory = false

        container.listBlobs(path).each { object ->
            if(object instanceof CloudBlobDirectory){
                directory = true
            }
        }

        return directory
    }

    @Override
    Map<String, String> getEntryMetadata(final String path) {
        CloudBlockBlob blob = getBlobFile(path)
        if(blob==null){
            return null
        }
        return AzureObjectStoreUtils.objectStatToMap(blob)
    }

    @Override
    Set<Resource<BaseStreamResource>> listSubDirectoriesAt(final String path) {
        def resources = []
        String lstPath = path == "" ? null : path

        List<CloudBlobDirectory> listBlobs = []

        container.listBlobs(lstPath).forEach{item->
            if(item instanceof CloudBlobDirectory){
                CloudBlobDirectory folder = (CloudBlobDirectory) item
                listBlobs.add(folder)
            }
        }

        listBlobs.each { result ->
            ListBlobItem item = (ListBlobItem) result
            resources.add(createSubDirectoryResourceListItemWithMetadata(result))
        }

        return resources
    }

    @Override
    Set<Resource<BaseStreamResource>> listEntriesAndSubDirectoriesAt(final String path) {
        def resources = []
        String lstPath = path == "" ? null : path

        List<CloudBlockBlob> listBlobEntries = []
        List<CloudBlobDirectory> listSubDirEntries = []

        container.listBlobs(lstPath).forEach{item->
            if(item instanceof CloudBlobDirectory){
                CloudBlobDirectory folder = (CloudBlobDirectory) item
                listSubDirEntries.add(folder)
            }
            else{
                listBlobEntries.add((CloudBlockBlob)item)
            }
        }

        listBlobEntries.each { result ->
            CloudBlockBlob blob = (CloudBlockBlob)result
            resources.add(createResourceListItemWithMetadata(blob))
        }
        listSubDirEntries.each { result ->
            resources.add(createSubDirectoryResourceListItemWithMetadata(result))
        }

        return resources
    }


    @Override
    Set<Resource<BaseStreamResource>> listResourceEntriesAt(final String path) {
        def resources = []
        String lstPath = path == "" ? null : path

        List<CloudBlockBlob> listBlobs = []
        List<CloudBlobDirectory> listSubDirEntries = []

        container.listBlobs(lstPath).forEach{item->
            if(item instanceof CloudBlobDirectory){
                CloudBlobDirectory folder = (CloudBlobDirectory) item
                listBlobs.addAll(AzureObjectStoreUtils.listBlobsFromDirectory(folder))
            }else{
                listBlobs.add((CloudBlockBlob)item)
            }
        }

        listBlobs.each { result ->
            CloudBlockBlob blob = (CloudBlockBlob)result
            resources.add(createResourceListItemWithMetadata(blob))
        }
        return resources
    }

    private AzureObjectStoreResource createResourceListItemWithMetadata(final CloudBlockBlob item) {
        BaseStreamResource content = new BaseStreamResource(getEntryMetadata(item.getName()),
                                     new LazyAccessObjectStoreInputStream(item))
        return new AzureObjectStoreResource(item.getName(), content)
    }

    private static AzureObjectStoreResource createSubDirectoryResourceListItemWithMetadata(final CloudBlobDirectory item) {
        return new AzureObjectStoreResource(item.getPrefix(), null, true)
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

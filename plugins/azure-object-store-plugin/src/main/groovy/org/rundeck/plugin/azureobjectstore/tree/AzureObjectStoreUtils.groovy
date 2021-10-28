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

import com.dtolabs.rundeck.core.storage.StorageUtil
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob


class AzureObjectStoreUtils {

    static Map<String,String> objectStatToMap(CloudBlockBlob objectStat) {
        objectStat.downloadAttributes()

        Map<String,String> meta = objectStat.getMetadata()
        Map<String,String> metaResult = [:]

        meta.each {key, value->
            String rundeckKey = key.replace("_","-")
            metaResult.put(fixKeyName(rundeckKey),value)
        }
        if(!metaResult.get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME)) {
            metaResult[StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME] = StorageUtil.formatDate(objectStat.getProperties().getLastModified())
        }
        if(!metaResult.get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)) {
            metaResult[StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME] = StorageUtil.formatDate(objectStat.getProperties().getLastModified())
        }

        return metaResult
    }


    static String cleanAzureKeyName(String prefixedKey) {
        String key = prefixedKey.replaceAll("-", "_")
        return key
    }

    private static String fixKeyName(String prefixedKey) {
        String key = prefixedKey.replaceAll(AzureObjectStoreTree.RUNDECK_CUSTOM_HEADER_PREFIX, "")
        return key.startsWith("rundeck") ? key.capitalize() : key
    }


    static CloudBlockBlob getBlobFile(CloudBlobContainer container, String path){
        CloudBlockBlob blob = container.getBlockBlobReference(path);
        return blob
    }

    static List<CloudBlockBlob> listBlobs(CloudBlobContainer container){
        List<CloudBlockBlob> listBlobs = []

        container.listBlobs().forEach{item->
            if(item instanceof CloudBlobDirectory){
                CloudBlobDirectory folder = (CloudBlobDirectory) item
                listBlobs.addAll(AzureObjectStoreUtils.listBlobsFromDirectory(folder))
            }else{
                listBlobs.add((CloudBlockBlob)item)
            }
        }
        listBlobs
    }

    static List<CloudBlockBlob> listBlobsFromDirectory(CloudBlobDirectory directory){
        List<CloudBlockBlob> listBlobs = []
        directory.listBlobs().each{item->
            if(item instanceof CloudBlobDirectory){
                CloudBlobDirectory folder = (CloudBlobDirectory) item
                listBlobs.addAll(listBlobsFromDirectory(folder))
            }else{
                listBlobs.add((CloudBlockBlob)item)
            }
        }

        return listBlobs
    }
}

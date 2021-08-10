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
import org.rundeck.storage.api.Resource


interface AzureObjectStoreDirectorySource {
    boolean checkPathExists(String path)
    boolean checkResourceExists(String path)
    boolean checkPathExistsAndIsDirectory(String path)
    Map<String,String> getEntryMetadata(String path)
    Set<Resource<BaseStreamResource>> listSubDirectoriesAt(String path)
    Set<Resource<BaseStreamResource>> listEntriesAndSubDirectoriesAt(String path)
    Set<Resource<BaseStreamResource>> listResourceEntriesAt(String path)
    void updateEntry(String fullEntryPath, Map<String,String> meta)
    void deleteEntry(String fullEntryPath)
    void resyncDirectory()
}

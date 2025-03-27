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
package com.rundeck.repository.client.repository

import com.dtolabs.rundeck.core.storage.StorageTree
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.RepositoryFactory
import com.rundeck.repository.api.RepositoryOwner
import com.rundeck.repository.api.RepositoryType
import com.rundeck.repository.api.ArtifactRepository

class RundeckRepositoryFactory implements RepositoryFactory {

    StorageTree repositoryStorageTree

    ArtifactRepository createRepository(RepositoryDefinition definition) {
        ArtifactRepository repository
        if(definition.type == RepositoryType.FILE) {
            repository = new FilesystemArtifactRepository(definition)
        } else if(definition.type == RepositoryType.STORAGE_TREE) {
            repository = new StorageTreeArtifactRepository(repositoryStorageTree, definition)
        } else if(definition.type == RepositoryType.HTTP && definition.owner == RepositoryOwner.RUNDECK) {
            repository = new RundeckHttpRepository(definition)
        } else {
            throw new Exception("Unknown repository type: ${definition.type}")
        }
        return repository
    }
}

/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;

import java.util.Set;

/**
 * Empty tree which has no content and throws exceptions.
 */
public class EmptyTree<T extends ContentMeta> extends StringToPathTree<T> {
    @Override
    public boolean hasPath(Path path) {
        return false;
    }

    @Override
    public boolean hasResource(Path path) {
        return false;
    }

    @Override
    public boolean hasDirectory(Path path) {
        return false;
    }

    @Override
    public Resource<T> getPath(Path path) {
        throw StorageException.readException(path, "Empty: no resource or directory for path: " + path);
    }

    @Override
    public Resource<T> getResource(Path path) {
        throw StorageException.readException(path, "Empty: no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        throw StorageException.listException(path, "Empty: no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        throw StorageException.listException(path, "Empty: no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        throw StorageException.listException(path, "Empty: no resource for path: " + path);
    }

    @Override
    public boolean deleteResource(Path path) {
        throw StorageException.deleteException(path, "Empty: no resource for path: " + path);
    }

    @Override
    public Resource<T> createResource(Path path, T content) {
        throw StorageException.createException(path, "Empty: cannot create resource: " + path);
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        throw StorageException.updateException(path, "Empty: no resource for path: " + path);
    }
}

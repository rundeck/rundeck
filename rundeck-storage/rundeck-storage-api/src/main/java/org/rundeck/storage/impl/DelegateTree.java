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

import org.rundeck.storage.api.*;

import java.util.Set;

/**
 * storage defers to a delegate
 */
public class DelegateTree<T extends ContentMeta> extends BaseDelegateTree<T> implements Tree<T> {
    public DelegateTree() {
    }

    public DelegateTree(Tree<T> delegate) {
        super(delegate);
    }

    @Override
    public boolean hasPath(Path path) {
        return getDelegate().hasPath(path);
    }

    @Override
    public boolean hasResource(Path path) {
        return getDelegate().hasResource(path);
    }

    @Override
    public boolean hasDirectory(Path path) {
        return getDelegate().hasDirectory(path);
    }

    @Override
    public Resource<T> getPath(Path path) {
        return getDelegate().getPath(path);
    }

    @Override
    public Resource<T> getResource(Path path) {
        return getDelegate().getResource(path);
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        return getDelegate().listDirectorySubdirs(path);
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        return getDelegate().listDirectoryResources(path);
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        return getDelegate().listDirectory(path);
    }

    @Override
    public boolean deleteResource(Path path) {
        return getDelegate().deleteResource(path);
    }

    @Override
    public  Resource<T> createResource(Path path, T content) {
        return getDelegate().createResource(path, content);
    }

    @Override
    public  Resource<T> updateResource(Path path, T content) {
        return getDelegate().updateResource(path, content);
    }

}

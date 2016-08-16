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
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.Tree;
import org.rundeck.storage.api.PathUtil;

import java.util.Set;

/**
 * Implements the String methods of Tree for subclassing
 */
public abstract class StringToPathTree<T extends ContentMeta> implements Tree<T> {

    @Override
    public boolean hasPath(String path) {
        return hasPath(PathUtil.asPath(path));
    }


    @Override
    public boolean hasResource(String path) {
        return hasResource(PathUtil.asPath(path));
    }

    @Override
    public boolean hasDirectory(String path) {
        return hasDirectory(PathUtil.asPath(path));
    }

    @Override
    public Resource<T> getPath(String path) {
        return getPath(PathUtil.asPath(path));
    }

    @Override
    public Resource<T> getResource(String path) {
        return getResource(PathUtil.asPath(path));
    }


    @Override
    public Set<Resource<T>> listDirectoryResources(String path) {
        return listDirectoryResources(PathUtil.asPath(path));
    }


    @Override
    public Set<Resource<T>> listDirectory(String path) {
        return listDirectory(PathUtil.asPath(path));
    }


    @Override
    public Set<Resource<T>> listDirectorySubdirs(String path) {
        return listDirectorySubdirs(PathUtil.asPath(path));
    }

    @Override
    public boolean deleteResource(String path) {
        return deleteResource(PathUtil.asPath(path));
    }

    @Override
    public Resource<T> createResource(String path, T content) {
        return createResource(PathUtil.asPath(path), content);
    }

    @Override
    public Resource<T> updateResource(String path, T content) {
        return updateResource(PathUtil.asPath(path), content);
    }
}

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

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.impl.StringToPathTree;

import java.util.Set;

/**
 * ResolvedExtTree provides a preset parameter to a {@link com.dtolabs.rundeck.core.storage.ExtTree} to provide {@link
 * StorageTree}.
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-04-02
 */
class ResolvedExtTree<S> extends StringToPathTree<ResourceMeta> implements StorageTree {
    private ExtTree<S, ResourceMeta> extTree;
    private S param;

    public ResolvedExtTree(ExtTree<S, ResourceMeta> extTree, S param) {
        this.extTree = extTree;
        this.param = param;
    }

    public static <S> ResolvedExtTree<S> with(S context, ExtTree<S, ResourceMeta> storageTree) {
        return new ResolvedExtTree<S>(storageTree, context);
    }

    @Override
    public boolean hasPath(Path path) {
        return extTree.hasPath(param, path);
    }

    @Override
    public boolean hasResource(Path path) {
        return extTree.hasResource(param, path);
    }

    @Override
    public boolean hasDirectory(Path path) {
        return extTree.hasDirectory(param, path);
    }

    @Override
    public Resource<ResourceMeta> getPath(Path path) {
        return extTree.getPath(param, path);
    }

    @Override
    public Resource<ResourceMeta> getResource(Path path) {
        return extTree.getResource(param, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(Path path) {
        return extTree.listDirectoryResources(param, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(Path path) {
        return extTree.listDirectory(param, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(Path path) {
        return extTree.listDirectorySubdirs(param, path);
    }

    @Override
    public boolean deleteResource(Path path) {
        return extTree.deleteResource(param, path);
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        return extTree.createResource(param, path, content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        return extTree.updateResource(param, path, content);
    }
}

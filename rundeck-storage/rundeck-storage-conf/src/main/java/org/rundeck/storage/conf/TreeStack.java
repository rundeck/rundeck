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

package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.impl.ResourceBase;

import java.util.*;

/**
 * tree that uses an ordered list of TreeHandlers to determine which underlying storage to use, and falls back to a
 * delegate if there is no match
 */
public class TreeStack<T extends ContentMeta> extends DelegateTree<T> {
    private List<? extends SelectiveTree<T>> treeHandlerList;

    public TreeStack(List<? extends SelectiveTree<T>> treeHandlerList, Tree<T> delegate) {
        super(delegate);
        validatePaths(treeHandlerList);
        this.treeHandlerList = sorted(treeHandlerList);
    }

    private void validatePaths(final List<? extends SelectiveTree<T>> treeHandlerList) {
        HashSet<String> paths = new HashSet<>();
        for (SelectiveTree<T> tSelectiveTree : treeHandlerList) {
            if (!paths.contains(tSelectiveTree.getSubPath().getPath())) {
                paths.add(tSelectiveTree.getSubPath().getPath());
            } else {
                throw new IllegalArgumentException(String.format(
                    "Cannot create TreeStack: multiple subpaths defined for: %s",
                    tSelectiveTree.getSubPath()
                ));
            }
        }
    }

    private List<? extends SelectiveTree<T>> sorted(final List<? extends SelectiveTree<T>> treeHandlerList) {
        ArrayList<? extends SelectiveTree<T>> list = new ArrayList<>(treeHandlerList);
        //sort by path length longest to shortest
        list.sort(new Comparator<SelectiveTree<T>>() {
            @Override
            public int compare(final SelectiveTree<T> o1, final SelectiveTree<T> o2) {
                return o2.getSubPath().getPath().length() - o1.getSubPath().getPath().length();
            }
        });
        return list;
    }

    @Override
    public Resource<T> getResource(Path path) {
        return getContentStorage(path).getResource(path);
    }

    @Override
    public Resource<T> getPath(Path path) {
        return getContentStorage(path).getPath(path);
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        return getContentStorage(path).listDirectoryResources(path);
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        //find substorage which are children of the given path
        return merge(listDirectoryIfFound(path), listStackDirectory(path));
    }

    private Set<Resource<T>> listDirectoryIfFound(Path path) {
        if(getContentStorage(path).hasDirectory(path)){
            return getContentStorage(path).listDirectory(path);
        }
        return null;
    }

    private Map<String, Resource<T>> asMap(Set<Resource<T>> matchedList) {
        HashMap<String, Resource<T>> map = new HashMap<>();
        for (Resource<T> tResource : matchedList) {
            map.put(tResource.getPath().getPath(), tResource);
        }
        return map;
    }

    private Set<Resource<T>> merge(Set<Resource<T>>... matchedList) {
        HashMap<String, Resource<T>> merge = new HashMap<>();
        if (null != matchedList && matchedList.length > 0) {
            for (Set<Resource<T>> resources : matchedList) {
                if (resources != null && resources.size() > 0) {
                    merge.putAll(asMap(resources));
                }
            }
        }
        return new HashSet<>(merge.values());
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        return merge(listDirectoryIfFound(path), listStackDirectory(path));
    }

    @Override
    public boolean deleteResource(Path path) {
        return getContentStorage(path).deleteResource(path);
    }

    @Override
    public Resource<T> createResource(Path path, T content) {
        return getContentStorage(path).createResource(path, content);
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        return getContentStorage(path).updateResource(path, content);
    }

    @Override
    public boolean hasPath(Path path) {
        return getContentStorage(path).hasPath(path);
    }

   public static boolean matchesPath(Path path, SelectiveTree<?> tree) {
        return path.equals(tree.getSubPath()) || PathUtil.hasRoot(path, tree.getSubPath());
    }

    /**
     * @param path parent path
     * @param tree tree with a subpath
     * @return true if the subpath is directly under the path
     */
    public static boolean hasParentPath(Path path, SubPath tree) {
        return path.equals(PathUtil.parentPath(tree.getSubPath()));
    }


    /**
     * List all treeHandlers as directories which have the given path as a parent
     * @param path path
     * @return
     */
    private Set<Resource<T>> listStackDirectory(Path path) {
        HashSet<Resource<T>> merge = new HashSet<Resource<T>>();
        if (treeHandlerList.size() > 0) {
            for (SelectiveTree<T> treeHandler : treeHandlerList) {
                if (PathUtil.hasRoot(treeHandler.getSubPath(), path) && !PathUtil.equals(
                    treeHandler.getSubPath(),
                    path
                )) {
                    String relativePath = PathUtil.removePrefix(path.getPath(), treeHandler.getSubPath().getPath());
                    String[] components = PathUtil.componentsFromPathString(relativePath);
                    Path subpath = PathUtil.appendPath(path, components[0]);
                    merge.add(new ResourceBase<T>(subpath, null, true));
                }
            }
        }
        return merge;
    }

    private Tree<T> getContentStorage(Path path) {
        if (treeHandlerList.size() > 0) {
            for (SelectiveTree<T> treeHandler : treeHandlerList) {
                if (matchesPath(path, treeHandler)) {
                    return treeHandler;
                }
            }
        }
        return getDelegate();
    }

    public boolean hasResource(Path path) {
        return getContentStorage(path).hasResource(path);
    }

    public boolean hasDirectory(Path path) {
        return getContentStorage(path).hasDirectory(path);
    }
}

package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.impl.ResourceBase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * tree that uses an ordered list of TreeHandlers to determine which underlying storage to use, and falls back to a
 * delegate if there is no match
 */
public class TreeStack<T extends ContentMeta> extends DelegateTree<T> {
    private List<? extends SelectiveTree<T>> treeHandlerList;

    public TreeStack(List<? extends SelectiveTree<T>> treeHandlerList, Tree<T> delegate) {
        super(delegate);
        this.treeHandlerList = treeHandlerList;
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

    private Set<Resource<T>> merge(Set<Resource<T>> matchedList, Set<Resource<T>> subList) {
        HashSet<Resource<T>> merge = new HashSet<Resource<T>>();
        if(null!=matchedList && matchedList.size()>0) {
            merge.addAll(matchedList);
        }
        if(null!=subList && subList.size()>0) {
            merge.addAll(subList);
        }
        return merge;
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

    public static boolean hasParentPath(Path path, SelectiveTree<?> tree) {
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
                if (hasParentPath(path, treeHandler)) {
                    Path subpath = PathUtil.appendPath(path, treeHandler.getSubPath().getName());
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

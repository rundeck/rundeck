package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.impl.DelegateTree;

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
        return getContentStorage(path).listDirectory(path);
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        return getContentStorage(path).listDirectorySubdirs(path);
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

    private Tree<T> getContentStorage(Path path) {
        if (treeHandlerList.size() > 0) {
            for (SelectiveTree<T> treeHandler : treeHandlerList) {
                if (treeHandler.matchesPath(path)) {
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

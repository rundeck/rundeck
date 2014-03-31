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

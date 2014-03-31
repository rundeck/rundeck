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

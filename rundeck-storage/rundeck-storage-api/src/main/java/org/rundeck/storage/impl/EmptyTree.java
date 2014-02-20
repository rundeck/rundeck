package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * $INTERFACE is ... User: greg Date: 2/18/14 Time: 2:17 PM
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
    public Resource<T> getResource(Path path) {
        throw new IllegalArgumentException("no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        throw new IllegalArgumentException("no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        throw new IllegalArgumentException("no resource for path: " + path);
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        throw new IllegalArgumentException("no resource for path: " + path);
    }

    @Override
    public boolean deleteResource(Path path) {
        throw new IllegalArgumentException("no resource for path: " + path);
    }

    @Override
    public Resource<T> createResource(Path path, T content) {
        throw new IllegalArgumentException("No tree storage");
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        throw new IllegalArgumentException("No tree storage");
    }
}

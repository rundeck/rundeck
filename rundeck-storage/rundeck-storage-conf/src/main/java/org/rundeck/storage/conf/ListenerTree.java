package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.impl.DelegateTree;

import java.util.Set;

/**
 * ListenerTree delegates operations to a tree delegate, and sends events matching the selectors to a listener
 *
 * @author greg
 * @since 2014-03-10
 */
public class ListenerTree<T extends ContentMeta> extends DelegateTree<T> {
    Listener<T> listener;
    PathSelector pathSelector;
    ResourceSelector<T> resourceSelector;

    public ListenerTree(Tree<T> delegate, Listener<T> listener, PathSelector pathSelector) {
        super(delegate);
        this.listener = listener;
        this.pathSelector = pathSelector;
    }

    public ListenerTree(Tree<T> delegate, Listener<T> listener, ResourceSelector<T> resourceSelector) {
        super(delegate);
        this.listener = listener;
        this.resourceSelector = resourceSelector;
    }

    public ListenerTree(Tree<T> delegate, Listener<T> listener, PathSelector pathSelector,
            ResourceSelector<T> resourceSelector) {
        super(delegate);
        this.listener = listener;
        this.pathSelector = pathSelector;
        this.resourceSelector = resourceSelector;
    }

    @Override
    public Resource<T> getPath(Path path) {
        Resource<T> path1 = super.getPath(path);
        if (matches(path, null != path1 ? path1.getContents() : null)) {
            listener.didGetPath(path, path1);
        }
        return path1;
    }

    private boolean matches(Path path, T content) {
        return MatcherUtil.matches(path, content, pathSelector, true, resourceSelector, true);
    }

    @Override
    public Resource<T> getResource(Path path) {
        Resource<T> resource = super.getResource(path);
        if (matches(path, resource.getContents())) {
            listener.didGetResource(path, resource);
        }
        return resource;
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        Set<Resource<T>> resources = super.listDirectorySubdirs(path);
        if (matches(path, null)) {
            listener.didListDirectorySubdirs(path, resources);
        }
        return resources;
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        Set<Resource<T>> resources = super.listDirectoryResources(path);
        if (matches(path, null)) {
            listener.didListDirectoryResources(path, resources);
        }
        return resources;
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        Set<Resource<T>> resources = super.listDirectory(path);

        if (matches(path, null)) {
            listener.didListDirectory(path, resources);
        }
        return resources;
    }

    @Override
    public boolean deleteResource(Path path) {
        boolean b = super.deleteResource(path);
        if (matches(path, null)) {
            listener.didDeleteResource(path, b);
        }
        return b;
    }

    @Override
    public Resource<T> createResource(Path path, T content) {
        Resource<T> resource = super.createResource(path, content);

        if (matches(path, content)) {
            listener.didCreateResource(path, content, resource);
        }
        return resource;
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        Resource<T> tResource = super.updateResource(path, content);

        if (matches(path, content)) {
            listener.didUpdateResource(path, content, tResource);
        }
        return tResource;
    }
}

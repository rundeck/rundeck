package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.impl.DelegateResource;
import org.rundeck.storage.impl.DelegateTree;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SelectiveTree that Maps resources into a delegate, and can optionally remove the path prefix before transfering
 */
public class SubPathTree<T extends ContentMeta> extends DelegateTree<T> implements SelectiveTree<T> {
    Path rootPath;
    private boolean fullPath;

    /**

     * @param delegate delegate tree
     * @param rootPath root path for the subtree
     * @param fullPath true if the root path should not be removed when accessing the delegate
     */
    public SubPathTree(Tree<T> delegate, String rootPath, boolean fullPath) {
        this(delegate, PathUtil.asPath(rootPath), fullPath);
    }

    public SubPathTree(Tree<T> delegate, Path rootPath, boolean fullPath) {
        super(delegate);
        this.rootPath = rootPath;
        this.fullPath = fullPath;
    }

    @Override
    public Path getSubPath() {
        return rootPath;
    }

    private Path translatePathInternal(Path extpath) {
        return PathUtil.asPath(translatePathInternal(extpath.getPath()));
    }

    private String translatePathInternal(String extpath) {
        if (fullPath) {
            return extpath;
        } else {
            return PathUtil.removePrefix(rootPath.getPath(), extpath);
        }
    }

    private Path translatePathExternal(Path extpath) {
        return PathUtil.asPath(translatePathExternal(extpath.getPath()));
    }

    /**
     * convert internal path to external
     *
     * @param intpath
     *
     * @return
     */
    private String translatePathExternal(String intpath) {
        if (fullPath) {
            return intpath;
        } else {
            return PathUtil.appendPath(rootPath.getPath(), intpath);
        }
    }

    @Override
    public boolean hasPath(Path path) {
        return isLocalRoot(path) || super.hasPath(translatePathInternal(path));
    }

    @Override
    public boolean hasResource(Path path) {
        return super.hasResource(translatePathInternal(path));
    }

    @Override
    public boolean hasDirectory(Path path) {
        return isLocalRoot(path) || super.hasDirectory(translatePathInternal(path));
    }

    private boolean isLocalRoot(Path path) {
        return PathUtil.isRoot(PathUtil.removePrefix(rootPath.getPath(), path.getPath()));
    }

    @Override
    public Resource<T> getResource(Path path) {
        return translateResourceExternal(super.getResource(translatePathInternal(path)));
    }

    @Override
    public Resource<T> getPath(Path path) {
        return translateResourceExternal(super.getPath(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        if(isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectory(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        if (isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectorySubdirs(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        if (isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectoryResources(translatePathInternal(path)));
    }

    private Set<Resource<T>> translateAllExternal(Set<Resource<T>> internal) {
        HashSet<Resource<T>> resources = new HashSet<Resource<T>>();
        for (Resource<T> resource : internal) {
            resources.add(translateResourceExternal(resource));
        }
        return resources;
    }

    @Override
    public boolean deleteResource(Path path) {
        return super.deleteResource(translatePathInternal(path));
    }

    static class translatedResource<T extends ContentMeta> extends DelegateResource<T> {
        Path newpath;

        translatedResource(Resource<T> delegate, Path newpath) {
            super(delegate);
            this.newpath = newpath;
        }

        @Override
        public Path getPath() {
            return newpath;
        }
    }

    /**
     * Expose a resource with a path that maps to external path
     *
     * @param resource
     *
     * @return
     */
    private Resource<T> translateResourceExternal(Resource<T> resource) {
        if (fullPath) {
            return resource;
        }
        return new translatedResource<T>(resource, translatePathExternal(resource.getPath()));
    }


    @Override
    public Resource<T> createResource(Path path, T data) {
        return translateResourceExternal(super.createResource(translatePathInternal(path), data));
    }

    @Override
    public Resource<T> updateResource(Path path, T data) {
        return translateResourceExternal(super.updateResource(translatePathInternal(path), data));
    }
}

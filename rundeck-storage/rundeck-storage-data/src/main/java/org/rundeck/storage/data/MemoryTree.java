package org.rundeck.storage.data;

import org.rundeck.storage.impl.ResourceBase;
import org.rundeck.storage.impl.StringToPathTree;
import org.rundeck.storage.api.*;
import org.rundeck.storage.api.PathUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores resources in memory
 */
public class MemoryTree<T extends ContentMeta> extends StringToPathTree<T> implements Tree<T> {

    static class DirRes<T extends ContentMeta> implements PathItem {
        private Path path;
        boolean dir;
        MyRes<T> res;
        Map<Path, DirRes<T>> dirList;

        DirRes(Path path) {
            this.path = path;
            dir = true;
            res = new MyRes<T>(path, null, true);
            dirList = new HashMap<Path, DirRes<T>>();
        }

        public Set<Resource<T>> dirListSet() {
            HashSet<Resource<T>> resources = new HashSet<Resource<T>>();
            for (DirRes<T> dirRes : dirList.values()) {
                if (!dirRes.dir) {
                    resources.add(dirRes.res);
                }
            }
            return resources;
        }

        public Set<Resource<T>> dirListAll() {
            HashSet<Resource<T>> dirs = new HashSet<Resource<T>>();
            for (DirRes<T> dirRes : dirList.values()) {
                dirs.add(dirRes.res);
            }
            return dirs;
        }

        public Set<Resource<T>> dirListDirs() {
            HashSet<Resource<T>> dirs = new HashSet<Resource<T>>();
            for (DirRes<T> dirRes : dirList.values()) {
                if (dirRes.dir) {
                    dirs.add(dirRes.res);
                }
            }
            return dirs;
        }

        DirRes(Path path, MyRes<T> res) {
            this.path = path;
            this.res = res;
            dir = false;
        }

        public Path getPath() {
            return path;
        }
    }

    /**
     * Root
     */
    DirRes<T> root = new DirRes<T>(PathUtil.ROOT);
    Map<Path, DirRes<T>> index = new HashMap<Path, DirRes<T>>();

    public MemoryTree() {
        index.put(PathUtil.asPath(""), root);
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        if (!hasDirectory(path)) {
            throw new IllegalArgumentException("No directory for path: " + path);
        }
        return index.get(path).dirListDirs();
    }

    @Override
    public boolean hasResource(Path path) {
        return hasPath(path) && !index.get(path).dir;
    }

    @Override
    public boolean hasPath(Path path) {
        return index.containsKey(path);
    }

    @Override
    public boolean hasDirectory(Path path) {
        return hasPath(path) && index.get(path).dir;
    }

    @Override
    public Resource<T> getResource(Path path) {
        if (!hasResource(path)) {
            throw new IllegalArgumentException("No resource for path: " + path);
        }
        return index.get(path).res;
    }

    @Override
    public Resource<T> getPath(Path path) {
        if (!hasPath(path)) {
            throw new IllegalArgumentException("No path: " + path);
        }
        return index.get(path).res;
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        if (!hasDirectory(path)) {
            throw new IllegalArgumentException("No directory for path: " + path);
        }
        return index.get(path).dirListSet();
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        if (!hasDirectory(path)) {
            throw new IllegalArgumentException("No directory for path: " + path);
        }
        return index.get(path).dirListAll();
    }

    @Override
    public boolean deleteResource(Path path) {
        if (!hasResource(path)) {
            return false;
        }
        DirRes<T> res = index.get(path);
        return deleteRes(res);
    }

    private boolean deleteRes(DirRes<T> dirRes) {
        boolean removed = false;
        if (!dirRes.dir) {
            Resource<T> res = dirRes.res;
            removed = index.remove(res.getPath()) != null;
            Path parentPath = PathUtil.parentPath(res.getPath());
            if (!PathUtil.isRoot(parentPath) && null != index.get(parentPath)) {
                DirRes<T> parentRes = index.get(parentPath);
                parentRes.dirList.remove(res.getPath());
                //remove parent dir if empty
                dirRes = parentRes;
            } else {
                return removed;
            }
        }

        while (null != dirRes && dirRes.dir && dirRes.dirList.size() < 1) {
            index.remove(dirRes.getPath());
            Path parentPath = PathUtil.parentPath(dirRes.getPath());
            if (parentPath != null && !PathUtil.isRoot(parentPath)) {
                DirRes<T> parentRes = index.get(parentPath);
                parentRes.dirList.remove(dirRes.getPath());
                dirRes = parentRes;
            } else {
                dirRes = null;
            }
        }
        return removed;
    }

    public Resource<T> createResource(Path path, T data) {
        if (hasResource(path)) {
            throw new IllegalArgumentException("Resource exists for path: " + path);
        }
        DirRes<T> newRes = createRes(path, data);
        return newRes.res;
    }

    @Override
    public Resource<T> updateResource(Path path, T data) {
        if (!hasResource(path)) {
            throw new IllegalArgumentException("Resource not found for path: " + path);
        }
        DirRes<T> dirRes = index.get(path);
        MyRes<T> resource = dirRes.res;
        resource.setContents(data);
        return resource;
    }

    static final class MyRes<T extends ContentMeta> extends ResourceBase<T> {
        MyRes(Path path, T content, boolean directory) {
            super(path, content, directory);
        }
        public void setContents(T contents){
            super.setContents(contents);
        }
    }

    private DirRes<T> createRes(Path path, T t) {
        DirRes<T> parent = createParentPaths(path);
        MyRes<T> resource = new MyRes<T>(path, t, false);
        DirRes<T> dirRes = new DirRes<T>(path, resource);
        parent.dirList.put(resource.getPath(), dirRes);
        index.put(path, dirRes);
        return dirRes;
    }

    private DirRes<T> createParentPaths(Path path) {
        String[] split = path.getPath().split("/");
        DirRes<T> current = root;
        Path currentPath = PathUtil.asPath("");
        for (int i = 0; i < split.length - 1; i++) {
            currentPath = PathUtil.appendPath(currentPath, split[i]);
            if (!index.containsKey(currentPath)) {
                DirRes<T> newDir = new DirRes<T>(currentPath);
                newDir.dir = true;
                current.dirList.put(currentPath, newDir);
                index.put(currentPath, newDir);
                current = newDir;
            } else {
                current = index.get(currentPath);
            }
        }
        return current;
    }


}

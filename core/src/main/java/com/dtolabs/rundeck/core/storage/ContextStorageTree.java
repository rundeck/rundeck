package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.impl.StringToPathTree;

import java.util.Set;

/**
 * ContextStorageTree uses an {@link AuthContext} and a {@link AuthStorageTree} to provide {@link StorageTree}.
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-04-02
 */
class ContextStorageTree extends StringToPathTree<ResourceMeta> implements StorageTree {
    private AuthStorageTree authRundeckStorageTree;
    private AuthContext authContext;

    public ContextStorageTree(AuthStorageTree authRundeckStorageTree, AuthContext authContext) {
        this.authRundeckStorageTree = authRundeckStorageTree;
        this.authContext = authContext;
    }
    public static ContextStorageTree with(AuthContext context, AuthStorageTree storageTree) {
        return new ContextStorageTree(storageTree, context);
    }

    @Override
    public boolean hasPath(Path path) {
        return authRundeckStorageTree.hasPath(authContext, path);
    }

    @Override
    public boolean hasResource(Path path) {
        return authRundeckStorageTree.hasResource(authContext, path);
    }

    @Override
    public boolean hasDirectory(Path path) {
        return authRundeckStorageTree.hasDirectory(authContext, path);
    }

    @Override
    public Resource<ResourceMeta> getPath(Path path) {
        return authRundeckStorageTree.getPath(authContext, path);
    }

    @Override
    public Resource<ResourceMeta> getResource(Path path) {
        return authRundeckStorageTree.getResource(authContext, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(Path path) {
        return authRundeckStorageTree.listDirectoryResources(authContext, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(Path path) {
        return authRundeckStorageTree.listDirectory(authContext, path);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(Path path) {
        return authRundeckStorageTree.listDirectorySubdirs(authContext, path);
    }

    @Override
    public boolean deleteResource(Path path) {
        return authRundeckStorageTree.deleteResource(authContext, path);
    }

    @Override
    public Resource<ResourceMeta> createResource(Path path, ResourceMeta content) {
        return authRundeckStorageTree.createResource(authContext, path, content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(Path path, ResourceMeta content) {
        return authRundeckStorageTree.updateResource(authContext, path, content);
    }
}

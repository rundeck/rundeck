package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;

import java.util.*;

/**
 * AuthRundeckStorageTree provides authorized access to a tree using an {@link AuthContext} for each request.
 *
 * @author greg
 * @since 2014-03-20
 */
public class AuthRundeckStorageTree implements AuthStorageTree {
    public static final String READ = "read";
    public static final String DELETE = "delete";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    public static final String STORAGE_PATH_AUTH_RES_TYPE = "storage";
    public static final String PATH_RES_KEY = "path";
    public static final String NAME_RES_KEY = "name";
    public static final String PROJECT_PATH_COMPONENT = "project";
    private StorageTree storageTree;

    public AuthRundeckStorageTree(StorageTree storageTree) {
        this.storageTree = storageTree;
    }

    /**
     * Evaluate access based on path
     *
     * @param context auth context
     * @param path    path
     * @param action  action
     *
     * @return true if authorized
     */
    private boolean authorizedPath(AuthContext context, Path path, String action) {
        Decision evaluate = context.evaluate(
                resourceForPath(path),
                action,
                environmentForPath(path)
        );
        return evaluate.isAuthorized();
    }

    /**
     * Return authorization resource map for a path
     *
     * @param path path
     *
     * @return map defining the authorization resource
     */
    private Map<String, String> resourceForPath(Path path) {
        return AuthorizationUtil.resource(STORAGE_PATH_AUTH_RES_TYPE, authResForPath(path));
    }

    /**
     * Map containing path and name given a path
     *
     * @param path path
     *
     * @return map
     */
    private Map<String, String> authResForPath(Path path) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        authResource.put(PATH_RES_KEY, path.getPath());
        authResource.put(NAME_RES_KEY, path.getName());
        return authResource;
    }

    /**
     * Generate the environment for a path, based on the convention that /project/name/* maps to a project called
     * "name", and anything else is within the application environment.
     *
     * @param path path
     *
     * @return authorization environment: a project environment if the path matches /project/name/*, otherwise the
     *         application environment
     */
    Set<Attribute> environmentForPath(Path path) {
        String[] paths = path.getPath().split("/");
        if (paths != null && paths.length > 2 && paths[0].equals(PROJECT_PATH_COMPONENT)) {
            return FrameworkProject.authorizationEnvironment(paths[1]);
        } else {
            return Framework.RUNDECK_APP_ENV;
        }
    }

    @Override
    public boolean hasPath(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && storageTree.hasPath(path);
    }

    @Override
    public boolean hasResource(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && storageTree.hasResource(path);
    }

    @Override
    public boolean hasDirectory(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && storageTree.hasDirectory(path);
    }

    @Override
    public Resource<ResourceMeta> getPath(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.READ, path);
        }

        Resource<ResourceMeta> resource = storageTree.getPath(path);
        if (resource.isDirectory()) {
            return resource;
        }

        return resource;
    }


    @Override
    public Resource<ResourceMeta> getResource(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.READ, path);
        }

        Resource<ResourceMeta> resource = storageTree.getResource(path);

        return resource;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.LIST, path);
        }
        Set<Resource<ResourceMeta>> resources = storageTree.listDirectoryResources(path);
        return filteredResources(auth, resources, READ);
    }

    private Set<Resource<ResourceMeta>> filteredResources(AuthContext auth,
            Set<Resource<ResourceMeta>> resources, String action) {
        HashSet<Resource<ResourceMeta>> resources1 = new HashSet<Resource<ResourceMeta>>();
        for (Resource<ResourceMeta> resource : resources) {
            if (authorizedPath(auth, resource.getPath(), action)) {
                resources1.add(resource);
            }
        }
        return resources1;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.LIST, path);
        }
        Set<Resource<ResourceMeta>> resources = storageTree.listDirectory(path);
        return filteredResources(auth, resources, READ);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.READ, path);
        }
        Set<Resource<ResourceMeta>> resources = storageTree.listDirectorySubdirs(path);
        return filteredResources(auth, resources, READ);
    }

    @Override
    public boolean deleteResource(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, DELETE)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.DELETE, path);
        }
        return storageTree.deleteResource(path);
    }

    @Override
    public Resource<ResourceMeta> createResource(AuthContext auth, Path path, ResourceMeta content) {
        if (!authorizedPath(auth, path, CREATE)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.CREATE, path);
        }
        return storageTree.createResource(path, withUsername(auth,content,true));
    }

    private ResourceMeta withUsername(AuthContext auth, ResourceMeta content, boolean create) {
        if(auth instanceof NamedAuthContext) {
            NamedAuthContext byUser = (NamedAuthContext) auth;
            ResourceMetaBuilder resourceMetaBuilder = StorageUtil.create(new HashMap<String,
                    String>(content.getMeta()));
            if(create){
                AuthStorageUsernameMeta.createResource(byUser, resourceMetaBuilder);
            }else{
                AuthStorageUsernameMeta.updateResource(byUser, resourceMetaBuilder);
            }
            return StorageUtil.withStream(content, resourceMetaBuilder.getResourceMeta());
        }
        return content;
    }

    @Override
    public Resource<ResourceMeta> updateResource(AuthContext auth, Path path, ResourceMeta content) {
        if (!authorizedPath(auth, path, UPDATE)) {
            throw new StorageAuthorizationException("Unauthorized access", StorageException.Event.UPDATE, path);
        }
        return storageTree.updateResource(path, withUsername(auth, content, false));
    }
}

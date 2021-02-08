/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.*;
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
    private StorageTree storageTree;
    private AuthStorageContextProvider contextProvider;

    public AuthRundeckStorageTree(StorageTree storageTree) {
        this.storageTree = storageTree;
        contextProvider = new ProjectKeyStorageContextProvider();
    }
    public AuthRundeckStorageTree(StorageTree storageTree, AuthStorageContextProvider authStorageContextProvider) {
        this.storageTree = storageTree;
        this.contextProvider=authStorageContextProvider;
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
    private boolean authorizedPath(AuthContext context, Path path, String action)
    {
        boolean authorized = false;
        Set<Attribute> environments = contextProvider.environmentForPath(path);
        for (Attribute env : environments)
        {
            Decision evaluate = context.evaluate(
                    resourceForPath(path),
                    action, Collections.singleton(env)

            );
            if(evaluate.explain() != null && evaluate.explain().getCode() == Explanation.Code.REJECTED_DENIED){
                return false;
            }

            if (evaluate.isAuthorized()){
                authorized = true;
            }
        }
        return authorized;
    }

    /**
     * Return authorization resource map for a path
     *
     * @param path path
     *
     * @return map defining the authorization resource
     */
    private Map<String, String> resourceForPath(Path path) {
        return AuthorizationUtil.resource(STORAGE_PATH_AUTH_RES_TYPE, contextProvider.authResForPath(path));
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

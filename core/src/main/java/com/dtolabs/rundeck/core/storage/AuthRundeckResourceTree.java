package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;

import java.util.*;

/**
 * AuthRundeckResourceTree provides authorized access to a tree using an {@link AuthContext} for each request.
 *
 * @author greg
 * @since 2014-03-20
 */
public class AuthRundeckResourceTree implements AuthResourceTree {
    public static final String READ = "read";
    public static final String DELETE = "delete";
    public static final String CREATE = "create";
    public static final String UPDATE = "update";
    private ResourceTree resourceTree;

    public AuthRundeckResourceTree(ResourceTree resourceTree) {
        this.resourceTree = resourceTree;
    }

    /**
     * Evaluate access based on path
     *
     * @param context
     * @param path
     * @param action
     *
     * @return
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
     * evaluate access based on content of a path
     *
     * @param auth
     * @param path1
     * @param action
     *
     * @return
     */
    private boolean authorizedContent(AuthContext auth, Resource<ResourceMeta> path1, String action) {
        Decision evaluate = auth.evaluate(
                resourceForContent(path1),
                action,
                environmentForPath(path1.getPath())
        );
        return evaluate.isAuthorized();
    }

    private Map<String, String> resourceForContent(Resource<ResourceMeta> content) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        authResource.put("type", "storageContent");
        authResource.put("path", content.getPath().getPath());
        authResource.put("name", content.getPath().getName());
        if (null != content.getContents() && null != content.getContents().getMeta()) {
            for (String s : content.getContents().getMeta().keySet()) {
                authResource.put(s, content.getContents().getMeta().get(s));
            }
        }
        return authResource;
    }

    private Map<String, String> resourceForPath(Path path) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        authResource.put("type", "storagePath");
        authResource.put("path", path.getPath());
        authResource.put("name", path.getName());
        return authResource;
    }

    Set<Attribute> environmentForPath(Path path) {
        String[] paths = path.getPath().split("/");
        if (paths != null && paths.length > 2 && paths[0].equals("project")) {
            return FrameworkProject.authorizationEnvironment(paths[1]);
        } else {
            return Framework.RUNDECK_APP_ENV;
        }
    }

    @Override
    public boolean hasPath(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && resourceTree.hasPath(path);
    }

    @Override
    public boolean hasResource(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && resourceTree.hasPath(path);
    }

    @Override
    public boolean hasDirectory(AuthContext auth, Path path) {
        return authorizedPath(auth, path, READ) && resourceTree.hasPath(path);
    }

    @Override
    public Resource<ResourceMeta> getPath(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw StorageException.readException(path, "Unauthorized access");
        }

        Resource<ResourceMeta> resource = resourceTree.getPath(path);
        if (resource.isDirectory()) {
            return resource;
        }

        if (!authorizedContent(auth, resource, READ)) {
            throw StorageException.readException(path, "Unauthorized access");
        }
        return resource;
    }


    @Override
    public Resource<ResourceMeta> getResource(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw StorageException.readException(path, "Unauthorized access");
        }

        Resource<ResourceMeta> resource = resourceTree.getResource(path);
        if (!authorizedContent(auth, resource, READ)) {
            throw StorageException.readException(path, "Unauthorized access");
        }

        return resource;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectoryResources(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw StorageException.listException(path, "Unauthorized access");
        }
        Set<Resource<ResourceMeta>> resources = resourceTree.listDirectoryResources(path);
        return filteredResources(auth, resources, READ);
    }

    private Set<Resource<ResourceMeta>> filteredResources(AuthContext auth,
            Set<Resource<ResourceMeta>> resources, String action) {
        HashSet<Resource<ResourceMeta>> resources1 = new HashSet<Resource<ResourceMeta>>();
        for (Resource<ResourceMeta> resource : resources) {
            if (resource.isDirectory() && authorizedPath(auth, resource.getPath(), action)
                    || !resource.isDirectory() && authorizedContent(auth, resource, action)) {
                resources1.add(resource);
            }
        }
        return resources1;
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectory(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw StorageException.listException(path, "Unauthorized access");
        }
        Set<Resource<ResourceMeta>> resources = resourceTree.listDirectory(path);
        return filteredResources(auth, resources, READ);
    }

    @Override
    public Set<Resource<ResourceMeta>> listDirectorySubdirs(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, READ)) {
            throw StorageException.listException(path, "Unauthorized access");
        }
        Set<Resource<ResourceMeta>> resources = resourceTree.listDirectorySubdirs(path);
        return filteredResources(auth, resources, READ);
    }

    @Override
    public boolean deleteResource(AuthContext auth, Path path) {
        if (!authorizedPath(auth, path, DELETE)) {
            throw StorageException.deleteException(path, "Unauthorized access");
        }
        return resourceTree.deleteResource(path);
    }

    @Override
    public Resource<ResourceMeta> createResource(AuthContext auth, Path path, ResourceMeta content) {
        if (!authorizedPath(auth, path, CREATE)) {
            throw StorageException.createException(path, "Unauthorized access");
        }
        return resourceTree.createResource(path, content);
    }

    @Override
    public Resource<ResourceMeta> updateResource(AuthContext auth, Path path, ResourceMeta content) {
        if (!authorizedPath(auth, path, UPDATE)) {
            throw StorageException.updateException(path, "Unauthorized access");
        }
        return resourceTree.updateResource(path, content);
    }
}

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * AuthResourceTree authenticated facade to {@link org.rundeck.storage.api.Tree}
 *
 * @author greg
 * @since 2014-03-20
 */
public interface AuthTree<T extends ContentMeta> {

    /**
     * Return true if the path exists
     *
     * @param path path
     *
     * @return true if it exists
     */
    boolean hasPath(AuthContext auth, Path path);


    /**
     * Return true if a resource at the path exists
     *
     * @param path path
     *
     * @return true if a resource exists
     */
    boolean hasResource(AuthContext auth, Path path);


    /**
     * Return true if a directory at the path exists
     *
     * @param path path
     *
     * @return true if the path is a directory
     */
    boolean hasDirectory(AuthContext auth, Path path);


    /**
     * Return the resource or directory at the path
     *
     * @param path path
     *
     * @return Resource or directory
     */
    Resource<T> getPath(AuthContext auth, Path path);


    /**
     * Return the resource at the path
     *
     * @param path path
     *
     * @return Resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> getResource(AuthContext auth, Path path);


    /**
     * Return the set of non-directory resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectoryResources(AuthContext auth, Path path);


    /**
     * Return the set of resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectory(AuthContext auth, Path path);


    /**
     * Return the set of sub directory resources within the directory path
     *
     * @param path directory path
     *
     * @return set of subdirectories
     */
    Set<Resource<T>> listDirectorySubdirs(AuthContext auth, Path path);


    /**
     * Delete a resource at a path
     *
     * @param path path
     *
     * @return true if the resource was deleted
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    boolean deleteResource(AuthContext auth, Path path);


    /**
     * Create a resource
     *
     * @param path path
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory
     */
    Resource<T> createResource(AuthContext auth, Path path, T content);


    /**
     * Update an existing resource
     *
     * @param path path
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> updateResource(AuthContext auth, Path path, T content);

}

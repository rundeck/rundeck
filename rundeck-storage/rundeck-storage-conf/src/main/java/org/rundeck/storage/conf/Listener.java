package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * Listener receives notification of events on a tree
 *
 * @author greg
 * @since 2014-03-10
 */
public interface Listener<T extends ContentMeta> {


    /**
     * Return the resource or directory at the path
     *
     * @param path path
     *
     * @return Resource or directory
     */

    void didGetPath(Path path, Resource<T> resource);


    /**
     * Return the resource at the path
     *
     * @param path path
     *
     * @return Resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */

    void didGetResource(Path path, Resource<T> resource);


    /**
     * Return the set of non-directory resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */

    void didListDirectoryResources(Path path, Set<Resource<T>> contents);


    /**
     * Return the set of resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */

    void didListDirectory(Path path, Set<Resource<T>> contents);


    /**
     * Return the set of sub directory resources within the directory path
     *
     * @param path directory path
     *
     * @return set of subdirectories
     */

    void didListDirectorySubdirs(Path path, Set<Resource<T>> contents);


    /**
     * Delete a resource at a path
     *
     * @param path path
     *
     * @return true if the resource was deleted
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */

    void didDeleteResource(Path path, boolean success);


    /**
     * Create a resource
     *
     * @param path path
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory
     */

    void didCreateResource(Path path, T content, Resource<T> contents);


    /**
     * Update an existing resource
     *
     * @param path path
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */

    void didUpdateResource(Path path, T content, Resource<T> contents);
}

package com.dtolabs.rundeck.core.storage;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * ExtTree extends each tree method with additional parameter
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-04-03
 */
public interface ExtTree<S, T extends ContentMeta> {

    /**
     * Return true if the path exists
     *
     * @param path path
     * @param extra extra component
     *
     * @return true if it exists
     */
    boolean hasPath(S extra, Path path);


    /**
     * Return true if a resource at the path exists
     *
     * @param path path
     * @param extra extra component
     *
     * @return true if a resource exists
     */
    boolean hasResource(S extra, Path path);


    /**
     * Return true if a directory at the path exists
     *
     * @param path path
     * @param extra extra component
     *
     * @return true if the path is a directory
     */
    boolean hasDirectory(S extra, Path path);


    /**
     * Return the resource or directory at the path
     *
     * @param path path
     * @param extra extra component
     *
     * @return Resource or directory
     */
    Resource<T> getPath(S extra, Path path);


    /**
     * Return the resource at the path
     *
     * @param path path
     * @param extra extra component
     *
     * @return Resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> getResource(S extra, Path path);


    /**
     * Return the set of non-directory resources at the directory path
     *
     * @param path path
     * @param extra extra component
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectoryResources(S extra, Path path);


    /**
     * Return the set of resources at the directory path
     *
     * @param path path
     * @param extra extra component
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectory(S extra, Path path);


    /**
     * Return the set of sub directory resources within the directory path
     *
     * @param path directory path
     * @param extra extra component
     *
     * @return set of subdirectories
     */
    Set<Resource<T>> listDirectorySubdirs(S extra, Path path);


    /**
     * Delete a resource at a path
     *
     * @param path path
     * @param extra extra component
     *
     * @return true if the resource was deleted
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    boolean deleteResource(S extra, Path path);


    /**
     * Create a resource
     *
     * @param path path
     * @param extra extra component
     * @param content content
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory
     */
    Resource<T> createResource(S extra, Path path, T content);


    /**
     * Update an existing resource
     *
     * @param path path
     * @param extra extra component
     * @param content content
     *
     * @return the resource
     *
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> updateResource(S extra, Path path, T content);
}

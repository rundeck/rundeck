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
     * @param resource resource
     */

    void didGetPath(Path path, Resource<T> resource);


    /**
     * Return the resource at the path
     *
     * @param path path
     * @param resource resource
     *
     *
     */

    void didGetResource(Path path, Resource<T> resource);


    /**
     * Return the set of non-directory resources at the directory path
     *
     * @param path path
     * @param contents set of contents
     *
     */

    void didListDirectoryResources(Path path, Set<Resource<T>> contents);


    /**
     * Return the set of resources at the directory path
     *
     * @param path path
     * @param contents set of contents
     *
     */

    void didListDirectory(Path path, Set<Resource<T>> contents);


    /**
     * Return the set of sub directory resources within the directory path
     *
     * @param path directory path
     * @param contents set of contents
     *
     */

    void didListDirectorySubdirs(Path path, Set<Resource<T>> contents);


    /**
     * Delete a resource at a path
     *
     * @param path path
     * @param success true if successful
     *
     *
     */

    void didDeleteResource(Path path, boolean success);


    /**
     * Created a resource
     *
     * @param path path
     *
     * @param content new content
     * @param contents new resource
     */

    void didCreateResource(Path path, T content, Resource<T> contents);


    /**
     * Updated an existing resource
     *
     * @param path path
     * @param content  new content
     * @param contents new resource
     *
     *
     */

    void didUpdateResource(Path path, T content, Resource<T> contents);
}

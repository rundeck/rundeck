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

package org.rundeck.storage.api;

import java.util.Set;

/**
 * Stores content in a hierarchy
 */
public interface Tree<T extends ContentMeta> {

    /**
     * Return true if the path exists
     *
     * @param path path
     *
     * @return true if it exists
     */
    boolean hasPath(Path path);

    boolean hasPath(String path);

    /**
     * Return true if a resource at the path exists
     *
     * @param path path
     *
     * @return true if a resource exists
     */
    boolean hasResource(Path path);

    boolean hasResource(String path);

    /**
     * Return true if a directory at the path exists
     *
     * @param path path
     *
     * @return true if the path is a directory
     */
    boolean hasDirectory(Path path);

    boolean hasDirectory(String path);

    /**
     * Return the resource or directory at the path
     *
     * @param path path
     *
     * @return Resource or directory
     */
    Resource<T> getPath(Path path);

    Resource<T> getPath(String path);

    /**
     * Return the resource at the path
     *
     * @param path path
     *
     * @return Resource
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> getResource(Path path);

    Resource<T> getResource(String path);

    /**
     * Return the set of non-directory resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectoryResources(Path path);

    Set<Resource<T>> listDirectoryResources(String path);

    /**
     * Return the set of resources at the directory path
     *
     * @param path path
     *
     * @return set of resources
     */
    Set<Resource<T>> listDirectory(Path path);

    Set<Resource<T>> listDirectory(String path);

    /**
     * Return the set of sub directory resources within the directory path
     *
     * @param path directory path
     *
     * @return set of subdirectories
     */
    Set<Resource<T>> listDirectorySubdirs(Path path);

    Set<Resource<T>> listDirectorySubdirs(String path);

    /**
     * Delete a resource at a path
     *
     * @param path path
     *
     * @return true if the resource was deleted
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    boolean deleteResource(Path path);

    boolean deleteResource(String path);

    /**
     * Create a resource
     *
     * @param path path
     * @param content resource content
     *
     * @return the resource
     * @throws IllegalArgumentException if the path is a directory
     */
    Resource<T> createResource(Path path, T content);

    Resource<T> createResource(String path, T content);


    /**
     * Update an existing resource
     *
     * @param path path
     * @param content resource content
     *
     * @return the resource
     * @throws IllegalArgumentException if the path is a directory or does not exist
     */
    Resource<T> updateResource(Path path, T content);

    Resource<T> updateResource(String path, T content);
}

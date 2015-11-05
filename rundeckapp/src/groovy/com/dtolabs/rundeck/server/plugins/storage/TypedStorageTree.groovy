package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource

/**
 * Extends StorageTree to provide content-type requirements for resources
 */
interface TypedStorageTree extends StorageTree {
    /**
     *
     * @param path path
     * @param contentType content type string
     * @return contents of the resource
     * @throws WrongContentType if the content type requested does not match
     * @throws org.rundeck.storage.api.StorageException if underlying tree throws an exception, e.g. not found
     */
    Resource<ResourceMeta> getResourceWithType(Path path, String contentType)
    /**
     *
     * @param path path
     * @param contentType content type string
     * @return true if the resource exists and has the specified content type
     * @throws WrongContentType if the content type requested does not match
     * @throws org.rundeck.storage.api.StorageException if underlying tree throws an exception, e.g. not found
     */
    boolean hasResourceWithType(Path path, String contentType)
    /**
     *
     * @param path path
     * @param contentType content type string
     * @return contents of the resource
     * @throws WrongContentType if the content type requested does not match
     * @throws org.rundeck.storage.api.StorageException if underlying tree throws an exception, e.g. not found
     */
    byte[] readResourceWithType(Path path, String contentType)
}
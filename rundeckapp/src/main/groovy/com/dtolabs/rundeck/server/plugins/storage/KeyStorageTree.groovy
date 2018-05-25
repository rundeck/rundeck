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

package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource

/**
 * Utility enhancements to provide password/key loading
 */
interface KeyStorageTree extends StorageTree {

    /**
     * @param path path
     * @return password resource
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPassword(Path path)

    /**
     *
     * @param path path
     * @return password data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPassword(Path path)

    /**
     *
     * @param path path
     * @return password data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPassword(String path)

    /**
     * @param path path
     * @return public key resource
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPublicKey(Path path)

    /**
     *
     * @param path path
     * @return public key data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPublicKey(Path path)

    /**
     *
     * @param path path
     * @return public key data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPublicKey(String path)

    /**
     * @param path path
     * @return private key resource
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPrivateKey(Path path)

    /**
     *
     * @param path path
     * @return private key data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPrivateKey(Path path)

    /**
     *
     * @param path path
     * @return private key data
     *
     * @throws WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPrivateKey(String path)

}
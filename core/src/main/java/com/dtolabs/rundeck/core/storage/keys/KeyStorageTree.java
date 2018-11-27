/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.storage.keys;

import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageTree;
import org.rundeck.app.spi.AppService;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.io.IOException;

/**
 * Utility enhancements to provide password/key loading
 */
public interface KeyStorageTree
    extends StorageTree, AppService
{

    /**
     * @param path path
     * @return password resource
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPassword(Path path);

    /**
     *
     * @param path path
     * @return password data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPassword(Path path) throws IOException;

    /**
     *
     * @param path path
     * @return password data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPassword(String path) throws IOException;

    /**
     * @param path path
     * @return public key resource
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPublicKey(Path path);

    /**
     *
     * @param path path
     * @return public key data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPublicKey(Path path) throws IOException;

    /**
     *
     * @param path path
     * @return public key data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPublicKey(String path) throws IOException;

    /**
     * @param path path
     * @return private key resource
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    Resource<ResourceMeta> getPrivateKey(Path path);

    /**
     *
     * @param path path
     * @return private key data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPrivateKey(Path path) throws IOException;

    /**
     *
     * @param path path
     * @return private key data
     *
     * @throws com.dtolabs.rundeck.core.storage.WrongContentType if not the right content type
     * @throws org.rundeck.storage.api.StorageException if not found
     */
    byte[] readPrivateKey(String path) throws IOException;

    /**
     *
     * @param path path
     * @return true if the resource exists and is the right content type
     *
     */
    boolean hasPassword(String path);

    /**
     * @param path path
     * @return true if the resource exists and is the right content type
     */
    boolean hasPrivateKey(String path);

    /**
     * @param path path
     * @return true if the resource exists and is the right content type
     */
    boolean hasPublicKey(String path);


}
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
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.Tree

/**
 * Implements {@link KeyStorageTree} and throws {@link WrongContentType} if
 * requested resource does not match the given content type.
 */
class KeyStorageTreeImpl extends TypedStorageTreeImpl implements KeyStorageTree {
    KeyStorageTreeImpl(final Tree<ResourceMeta> delegate) {
        super(delegate)
    }

    @Override
    Resource<ResourceMeta> getPassword(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE)
    }


    @Override
    byte[] readPassword(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE)
    }


    @Override
    byte[] readPassword(final String path) {
        return readPassword(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPrivateKey(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPrivateKey(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPrivateKey(final String path) {
        readPrivateKey(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPublicKey(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPublicKey(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPublicKey(final String path) {
        readPublicKey(PathUtil.asPath(path))
    }
}

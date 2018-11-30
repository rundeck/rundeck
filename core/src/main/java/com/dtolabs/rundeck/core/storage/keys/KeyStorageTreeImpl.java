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

import com.dtolabs.rundeck.core.storage.KeyStorageLayer;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.TypedStorageTreeImpl;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.Tree;

import java.io.IOException;

/**
 * Implements {@link KeyStorageTree} and throws {@link com.dtolabs.rundeck.core.storage.WrongContentType} if
 * requested resource does not match the given content type.
 */
class KeyStorageTreeImpl extends TypedStorageTreeImpl
    implements KeyStorageTree {
    KeyStorageTreeImpl(final Tree<ResourceMeta> delegate) {
        super(delegate);
    }

    @Override
    public Resource<ResourceMeta> getPassword(final Path path) {
        return getResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE);
    }


    @Override
    public byte[] readPassword(final Path path) throws IOException {
        return readResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE);
    }


    @Override
    public byte[] readPassword(final String path) throws IOException {
        return readPassword(PathUtil.asPath(path));
    }

    @Override
    public Resource<ResourceMeta> getPrivateKey(final Path path) {
        return getResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE);
    }

    @Override
    public byte[] readPrivateKey(final Path path) throws IOException {
        return readResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE);
    }

    @Override
    public byte[] readPrivateKey(final String path) throws IOException {
        return readPrivateKey(PathUtil.asPath(path));
    }

    @Override
    public boolean hasPassword(String path) {
        return hasResourceWithType(PathUtil.asPath(path), KeyStorageLayer.PASSWORD_MIME_TYPE);
    }

    @Override
    public boolean hasPrivateKey(String path) {
        return hasResourceWithType(PathUtil.asPath(path), KeyStorageLayer.PRIVATE_KEY_MIME_TYPE);
    }

    @Override
    public boolean hasPublicKey(String path) {
        return hasResourceWithType(PathUtil.asPath(path), KeyStorageLayer.PUBLIC_KEY_MIME_TYPE);
    }

    @Override
    public Resource<ResourceMeta> getPublicKey(final Path path) {
        return getResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE);
    }

    @Override
    public byte[] readPublicKey(final Path path) throws IOException {
        return readResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE);
    }

    @Override
    public byte[] readPublicKey(final String path) throws IOException {
        return readPublicKey(PathUtil.asPath(path));
    }
}

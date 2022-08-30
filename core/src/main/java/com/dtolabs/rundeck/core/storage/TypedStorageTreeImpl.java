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

package com.dtolabs.rundeck.core.storage;


import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;
import org.rundeck.storage.api.StorageException;
import org.rundeck.storage.api.Tree;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Read resources and specify a content type
 */
public class TypedStorageTreeImpl extends StorageTreeImpl implements TypedStorageTree {
    public TypedStorageTreeImpl(final Tree<ResourceMeta> delegate) {
        super(delegate);
    }

    private static byte[] readBytes(Resource<ResourceMeta> resource) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resource.getContents().writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    @Override
    public Resource<ResourceMeta> getResourceWithType(Path path, String contentType) {
        Resource<ResourceMeta> resource = getResource(path);
        if (resource.getContents().getContentType().equals(contentType)) {
            return resource;
        }
        String errorMessage = "Path " + path + " does not store a password, content-type: " + contentType;
        throw new WrongContentType(
                errorMessage,
                StorageException.Event.READ,
                path
        );
    }

    @Override
    public boolean hasResourceWithType(final Path path, final String contentType) {
        boolean exists = hasResource(path);
        if (!exists) {
            return false;
        }
        Resource<ResourceMeta> resource = getResource(path);

        return resource.getContents().getContentType().equals(contentType);
    }

    @Override
    public byte[] readResourceWithType(Path path, String contentType) throws IOException {
        return readBytes(getResourceWithType(path, contentType));
    }
}

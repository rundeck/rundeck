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
import com.dtolabs.rundeck.core.storage.StorageTreeImpl
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.api.Tree

/**
 * Read resources and specify a content type
 */
class TypedStorageTreeImpl extends StorageTreeImpl implements TypedStorageTree {
    TypedStorageTreeImpl(final Tree<ResourceMeta> delegate) {
        super(delegate)
    }

    private static byte[] readBytes(Resource<ResourceMeta> resource) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resource.contents.writeContent(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    @Override
    public Resource<ResourceMeta> getResourceWithType(Path path, String contentType) {
        def resource = getResource(path)
        if (resource.contents.contentType == contentType) {
            return resource
        }
        throw new WrongContentType(
                "Path ${path} does not store a password, content-type: ${resource.contents.contentType}",
                StorageException.Event.READ,
                path
        )
    }

    @Override
    boolean hasResourceWithType(final Path path, final String contentType) {
        def exists = hasResource(path)
        if (!exists) {
            return false
        }
        def resource = getResource(path)

        return resource.contents.contentType == contentType
    }

    @Override
    public byte[] readResourceWithType(Path path, String contentType) {
        return readBytes(getResourceWithType(path, contentType))
    }
}

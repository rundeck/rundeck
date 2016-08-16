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

import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path

/**
 * KeyStorageLayer applies content-type requirements
 * @author greg
 * @since 2014-03-19
 */
class KeyStorageLayer implements StorageConverterPlugin {
    public static final String PRIVATE_KEY_MIME_TYPE = "application/octet-stream"
    public static final String PUBLIC_KEY_MIME_TYPE = "application/pgp-keys"
    public static final String PASSWORD_MIME_TYPE = "application/x-rundeck-data-password"
    public static final String RUNDECK_KEY_TYPE = "Rundeck-key-type"
    public static final String RUNDECK_DATA_TYPE = "Rundeck-data-type"

    @Override
    HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        return null
    }

    @Override
    HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validate(resourceMetaBuilder, path)
        return null;
    }

    protected void validate(ResourceMetaBuilder resourceMetaBuilder, Path path) {
        def type = resourceMetaBuilder.contentType

        if (type.equals(PRIVATE_KEY_MIME_TYPE)) {
            resourceMetaBuilder.setMeta('Rundeck-content-mask', 'content')
            resourceMetaBuilder.setMeta(RUNDECK_KEY_TYPE, 'private')
        }else if (type.equals(PUBLIC_KEY_MIME_TYPE)){
            resourceMetaBuilder.setMeta(RUNDECK_KEY_TYPE, 'public')
        }else if (type.equals(PASSWORD_MIME_TYPE)){
            resourceMetaBuilder.setMeta('Rundeck-content-mask', 'content')
            resourceMetaBuilder.setMeta(RUNDECK_DATA_TYPE, 'password')
        }
    }

    @Override
    HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validate(resourceMetaBuilder, path)
        return null
    }
}

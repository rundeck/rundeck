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

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

import java.util.Date;

/**
 * StorageTimestamperConverter sets modification and creation timestamp metadata for updated/created resources.
 *
 * @author greg
 * @since 2014-03-16
 */
public class StorageTimestamperConverter implements StorageConverterPlugin {
    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        resourceMetaBuilder.setCreationTime(new Date());
        resourceMetaBuilder.setModificationTime(new Date());
        return null;
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        resourceMetaBuilder.setModificationTime(new Date());
        return null;
    }
}

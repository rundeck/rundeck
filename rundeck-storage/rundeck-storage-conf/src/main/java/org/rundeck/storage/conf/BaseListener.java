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

package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * BaseListener provides noop listener implementation.
 *
 * @author greg
 * @since 2014-03-14
 */
public class BaseListener<T extends ContentMeta> implements Listener<T> {
    @Override
    public void didGetPath(Path path, Resource<T> resource) {
    }

    @Override
    public void didGetResource(Path path, Resource<T> resource) {
    }

    @Override
    public void didListDirectoryResources(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didListDirectory(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didListDirectorySubdirs(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didDeleteResource(Path path, boolean success) {
    }

    @Override
    public void didCreateResource(Path path, T content, Resource<T> contents) {
    }

    @Override
    public void didUpdateResource(Path path, T content, Resource<T> contents) {
    }
}

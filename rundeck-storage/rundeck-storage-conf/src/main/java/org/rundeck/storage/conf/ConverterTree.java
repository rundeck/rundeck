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

import org.rundeck.storage.impl.DelegateResource;
import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.api.*;

/**
 * Tree that can convert resource content with a {@link ContentConverter}
 */
public class ConverterTree<T extends ContentMeta> extends DelegateTree<T> {
    ContentConverter<T> converter;
    PathSelector pathSelector;
    ResourceSelector<T> resourceSelector;

    /**
     * Convert content from the delegate with the given converter if it matches the selector
     *
     * @param delegate     delegate
     * @param converter    converter
     * @param pathSelector path selection
     */
    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, PathSelector pathSelector) {
        this(delegate, converter, pathSelector, null);
    }

    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, ResourceSelector<T> resourceSelector) {
        this(delegate, converter, null, resourceSelector);
    }

    public ConverterTree(Tree<T> delegate, ContentConverter<T> converter, PathSelector pathSelector,
            ResourceSelector<T> resourceSelector) {
        super(delegate);
        this.converter = converter;
        this.pathSelector = pathSelector;
        this.resourceSelector = resourceSelector;
    }


    private T filterReadData(Path path, T contents) {
        return null != converter ? converter.convertReadData(path, contents) : contents;
    }

    private T filterCreateData(Path path, T contents) {
        return null != converter ? converter.convertCreateData(path, contents) : contents;
    }

    private T filterUpdateData(Path path, T content) {
        return null != converter ? converter.convertUpdateData(path, content) : content;
    }

    @Override
    public Resource<T> getResource(Path path) {
        final Resource<T> resource = super.getResource(path);
        if (!resource.isDirectory() && MatcherUtil.matches(path, resource.getContents(), pathSelector, resourceSelector)) {
            return filterGetResource(path, resource);
        }
        return resource;
    }

    @Override
    public Resource<T> getPath(Path path) {
        final Resource<T> resource = super.getPath(path);
        if (!resource.isDirectory() && MatcherUtil.matches(path, resource.getContents(), pathSelector, resourceSelector)) {
            return filterGetResource(path, resource);
        }
        return resource;
    }

    

    private Resource<T> filterGetResource(final Path path, final Resource<T> resource) {
        return new DelegateResource<T>(resource) {
            @Override
            public T getContents() {
                return filterReadData(path, resource.getContents());
            }
        };
    }


    @Override
    public Resource<T> createResource(Path path, T content) {
        if (MatcherUtil.matches(path, content, pathSelector, resourceSelector)) {
            Resource<T> created=super.createResource(path, filterCreateData(path, content));
            return filterGetResource(path, created);
        }
        return super.createResource(path, content);
    }

    @Override
    public Resource<T> updateResource(Path path, T content) {
        if (MatcherUtil.matches(path, content, pathSelector, resourceSelector)) {
            Resource<T> updated = super.updateResource(path, filterUpdateData(path, content));
            return filterGetResource(path, updated);
        }
        return super.updateResource(path, content);
    }

}

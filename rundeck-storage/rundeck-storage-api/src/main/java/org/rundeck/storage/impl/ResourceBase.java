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

package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

/**
 * $INTERFACE is ... User: greg Date: 2/14/14 Time: 12:51 PM
 */
public class ResourceBase<T extends ContentMeta> extends PathItemBase implements Resource<T> {
    private T contents;
    boolean directory;

    public ResourceBase(Path path, T contents, boolean directory) {
        super(path);
        this.setContents(contents);
        this.directory = directory;
    }

    @Override
    public T getContents() {
        return contents;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    protected void setContents(T contents) {
        this.contents = contents;
    }
}

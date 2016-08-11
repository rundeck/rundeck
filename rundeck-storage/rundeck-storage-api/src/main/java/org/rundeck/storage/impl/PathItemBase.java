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

import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathItem;

/**
 * $INTERFACE is ... User: greg Date: 2/14/14 Time: 12:55 PM
 */
public abstract class PathItemBase implements PathItem {

    private Path path;

    public PathItemBase(Path path) {
        this.path = path;
    }

    @Override
    public Path getPath() {
        return path;
    }

}

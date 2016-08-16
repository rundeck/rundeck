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

import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.StorageException;

/**
 * StorageAuthorizationException indicates unauthorized request to the storage layer.
 *
 * @author greg
 * @since 2014-03-25
 */
public class StorageAuthorizationException extends StorageException {
    public StorageAuthorizationException(Event event, Path path) {
        super(event, path);
    }

    public StorageAuthorizationException(String s, Event event, Path path) {
        super(s, event, path);
    }

    public StorageAuthorizationException(String s, Throwable throwable, Event event, Path path) {
        super(s, throwable, event, path);
    }

    public StorageAuthorizationException(Throwable throwable, Event event, Path path) {
        super(throwable, event, path);
    }
}

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

import org.rundeck.storage.api.Path
import org.rundeck.storage.api.StorageException

/**
 * Resource content type was not correct
 */
class WrongContentType extends StorageException {
    WrongContentType(final StorageException.Event event, final Path path) {
        super(event, path)
    }

    WrongContentType(final String s, final StorageException.Event event, final Path path) {
        super(s, event, path)
    }

    WrongContentType(final String s, final Throwable throwable, final StorageException.Event event, final Path path) {
        super(s, throwable, event, path)
    }

    WrongContentType(final Throwable throwable, final StorageException.Event event, final Path path) {
        super(throwable, event, path)
    }
}

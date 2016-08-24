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

package org.rundeck.storage.api;

/**
 * An exception in the storage layer
 *
 * @author greg
 * @since 2014-03-10
 */
public class StorageException extends RuntimeException {
    private Event event;
    private Path path;

    public StorageException(Event event, Path path) {
        this.event = event;
        this.path = path;
    }

    public StorageException(String s, Event event, Path path) {
        super(s);
        this.event = event;
        this.path = path;
    }

    public StorageException(String s, Throwable throwable, Event event, Path path) {
        super(s, throwable);
        this.event = event;
        this.path = path;
    }

    public StorageException(Throwable throwable, Event event, Path path) {
        super(throwable);
        this.event = event;
        this.path = path;
    }

    public static StorageException readException(Path path, String message) {
        return new StorageException(message, Event.READ, path);
    }

    public static StorageException readException(Path path, String message, Throwable cause) {
        return new StorageException(message, cause, Event.READ, path);
    }

    public static StorageException updateException(Path path, String message) {
        return new StorageException(message, Event.UPDATE, path);
    }

    public static StorageException updateException(Path path, String message, Throwable cause) {
        return new StorageException(message, cause, Event.UPDATE, path);
    }

    public static StorageException createException(Path path, String message) {
        return new StorageException(message, Event.CREATE, path);
    }

    public static StorageException createException(Path path, String message, Throwable cause) {
        return new StorageException(message, cause, Event.CREATE, path);
    }

    public static StorageException deleteException(Path path, String message) {
        return new StorageException(message, Event.DELETE, path);
    }

    public static StorageException deleteException(Path path, String message, Throwable cause) {
        return new StorageException(message, cause, Event.DELETE, path);
    }

    public static StorageException listException(Path path, String message) {
        return new StorageException(message, Event.LIST, path);
    }

    public static StorageException listException(Path path, String message, Throwable cause) {
        return new StorageException(message, cause, Event.LIST, path);
    }

    public Event getEvent() {
        return event;
    }

    public Path getPath() {
        return path;
    }

    public static enum Event {
        READ,
        CREATE,
        UPDATE,
        DELETE,
        LIST
    }
}

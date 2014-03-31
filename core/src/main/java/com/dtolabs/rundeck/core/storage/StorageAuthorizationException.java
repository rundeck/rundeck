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

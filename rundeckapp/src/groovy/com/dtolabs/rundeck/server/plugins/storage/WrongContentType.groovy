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

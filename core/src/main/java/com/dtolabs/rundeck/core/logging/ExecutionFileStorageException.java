package com.dtolabs.rundeck.core.logging;

/**
 * Indicates an error with a log storage request
 */
public class ExecutionFileStorageException extends Exception {
    public ExecutionFileStorageException() {
    }

    public ExecutionFileStorageException(String s) {
        super(s);
    }

    public ExecutionFileStorageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ExecutionFileStorageException(Throwable throwable) {
        super(throwable);
    }
}

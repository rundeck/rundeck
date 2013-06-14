package com.dtolabs.rundeck.core.logging;

/**
 * Indicates an error with a log storage request
 */
public class LogFileStorageException extends Exception {
    public LogFileStorageException() {
    }

    public LogFileStorageException(String s) {
        super(s);
    }

    public LogFileStorageException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LogFileStorageException(Throwable throwable) {
        super(throwable);
    }
}

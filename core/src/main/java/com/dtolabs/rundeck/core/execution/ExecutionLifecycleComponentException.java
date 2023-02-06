package com.dtolabs.rundeck.core.execution;

public class ExecutionLifecycleComponentException extends Exception{

    public ExecutionLifecycleComponentException() {
    }

    public ExecutionLifecycleComponentException(String message) {
        super(message);
    }

    public ExecutionLifecycleComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionLifecycleComponentException(Throwable cause) {
        super(cause);
    }

    public ExecutionLifecycleComponentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

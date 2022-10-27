package com.dtolabs.rundeck.core.jobs;


public class JobLifecycleComponentException extends Exception {

    public JobLifecycleComponentException() {
        super();
    }

    public JobLifecycleComponentException(String msg) {
        super(msg);
    }

    public JobLifecycleComponentException(Exception cause) {
        super(cause);
    }

    public JobLifecycleComponentException(String msg, Exception cause) {
        super(msg, cause);
    }

}

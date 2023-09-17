package com.dtolabs.rundeck.core.plugins;


import com.dtolabs.rundeck.core.jobs.JobLifecycleComponentException;

public class JobLifecyclePluginException extends JobLifecycleComponentException {

    public JobLifecyclePluginException() {
        super();
    }

    public JobLifecyclePluginException(String msg) {
        super(msg);
    }

    public JobLifecyclePluginException(Exception cause) {
        super(cause);
    }

    public JobLifecyclePluginException(String msg, Exception cause) {
        super(msg, cause);
    }

}

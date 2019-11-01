package com.dtolabs.rundeck.core.plugins;


public class JobLifecyclePluginException extends Exception {

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

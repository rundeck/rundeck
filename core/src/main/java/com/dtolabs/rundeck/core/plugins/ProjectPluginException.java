package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

public class ProjectPluginException extends Exception {

    public ProjectPluginException() {
        super();
    }

    public ProjectPluginException(String msg) {
        super(msg);
    }

    public ProjectPluginException(Exception cause) {
        super(cause);
    }

    public ProjectPluginException(String msg, Exception cause) {
        super(msg, cause);
    }

}

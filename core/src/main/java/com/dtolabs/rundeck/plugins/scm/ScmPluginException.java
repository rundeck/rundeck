package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 8/25/15.
 */
public class ScmPluginException extends Exception {
    public ScmPluginException() {
    }

    public ScmPluginException(final String message) {
        super(message);
    }

    public ScmPluginException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ScmPluginException(final Throwable cause) {
        super(cause);
    }

    public ScmPluginException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace
    )
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

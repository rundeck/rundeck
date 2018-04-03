package com.dtolabs.rundeck.core.common;

/**
 * Created by rodrigo on 30-01-18.
 */
public class PluginDisabledException extends RuntimeException {

    private String message;

    PluginDisabledException(final String message) {
        super(message);
        this.message = message;
    }
}

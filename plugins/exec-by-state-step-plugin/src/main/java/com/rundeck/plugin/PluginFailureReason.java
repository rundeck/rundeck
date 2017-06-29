package com.rundeck.plugin;


import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

public enum PluginFailureReason implements FailureReason {
    /**
     * Driver not found in classpath
     */
    DriverNotFound,
    /**
     * Cant connect to the Database
     */
    ConnectDB,
    /**
     * Sql error
     */
    SQLFailure,
    /**
     * Missing mandatory Property
     */
    MissingProperty,

}

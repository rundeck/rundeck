package com.dtolabs.rundeck.app.support

import groovy.transform.CompileStatic

/**
 * Configuration for execution cleaner job feature
 */
@CompileStatic
interface ExecutionCleanerConfig {
    boolean isEnabled()
    int getMaxDaysToKeep()
    String getCronExpression()
    int getMinimumExecutionToKeep()
    int getMaximumDeletionSize()
}

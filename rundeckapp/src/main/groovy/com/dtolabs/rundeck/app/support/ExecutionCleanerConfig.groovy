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

    /**
     * @return number of most recent completed executions to keep for each job, 0 to disable
     */
    int getExecutionsToKeepPerJob()
}

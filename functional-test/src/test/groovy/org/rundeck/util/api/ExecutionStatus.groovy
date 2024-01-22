package org.rundeck.util.api

enum ExecutionStatus {
    SUCCEEDED("succeeded"),
    FAILED_WITH_RETRY("failed-with-retry"),
    RUNNING("running"),
    FAILED("failed")

    String state

    ExecutionStatus(String state) {
        this.state = state
    }
}

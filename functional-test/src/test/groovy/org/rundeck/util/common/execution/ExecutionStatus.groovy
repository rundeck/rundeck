package org.rundeck.util.common.execution

enum ExecutionStatus {
    SUCCEEDED("succeeded"),
    FAILED_WITH_RETRY("failed-with-retry"),
    RUNNING("running"),
    FAILED("failed"),
    TIMEDOUT("timedout")

    String state

    ExecutionStatus(String state) {
        this.state = state
    }
}

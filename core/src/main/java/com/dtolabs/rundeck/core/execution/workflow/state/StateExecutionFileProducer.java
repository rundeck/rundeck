package com.dtolabs.rundeck.core.execution.workflow.state;

import org.rundeck.app.services.ExecutionFileProducer;

public interface StateExecutionFileProducer extends ExecutionFileProducer {
    String STATE_FILE_FILETYPE = "state.json";

    default String getExecutionFileType() {
        return STATE_FILE_FILETYPE;
    }
}

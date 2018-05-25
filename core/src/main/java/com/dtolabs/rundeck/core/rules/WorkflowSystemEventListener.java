package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 5/18/16.
 */
public interface WorkflowSystemEventListener {
    void onEvent(WorkflowSystemEvent event);
}

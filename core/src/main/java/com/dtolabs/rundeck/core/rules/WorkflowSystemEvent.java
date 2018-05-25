package com.dtolabs.rundeck.core.rules;

/**
 * Created by greg on 5/18/16.
 */
public interface WorkflowSystemEvent {
    public String getMessage();

    public WorkflowSystemEventType getEventType();

    public Object getData();
}

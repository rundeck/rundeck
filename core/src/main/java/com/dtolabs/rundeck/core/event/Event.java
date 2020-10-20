package com.dtolabs.rundeck.core.event;

public interface Event {
        String getProjectName();
        String getSubsystem();
        String getTopic();
        String getObjectId();
        Object getMeta();
}

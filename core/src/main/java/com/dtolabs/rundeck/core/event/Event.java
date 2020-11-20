package com.dtolabs.rundeck.core.event;


public interface Event {
        String getProjectName();
        String getSubsystem();
        String getTopic();
        String getObjectId();
        Long getSequence();
        Object getMeta();
}

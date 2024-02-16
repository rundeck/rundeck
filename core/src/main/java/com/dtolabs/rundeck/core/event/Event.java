package com.dtolabs.rundeck.core.event;


import org.rundeck.app.data.model.v1.storedevent.StoredEventData;

public interface Event extends StoredEventData {
        String getProjectName();
        String getSubsystem();
        String getTopic();
        String getObjectId();
        Long getSequence();
        Object getMeta();
}

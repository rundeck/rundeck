package com.dtolabs.rundeck.core.event;

import lombok.Data;
import org.rundeck.app.data.model.v1.storedevent.EventSeverity;

import java.io.Serializable;
import java.util.Date;

@Data
public class EventImpl implements Event {
    String projectName;
    String subsystem;
    String topic;
    String objectId;
    Long sequence;
    Object meta;

    Serializable id;
    String serverUUID;
    EventSeverity severity;
    Date lastUpdated;
    int schemaVersion;
}

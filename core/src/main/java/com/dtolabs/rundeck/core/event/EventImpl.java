package com.dtolabs.rundeck.core.event;

import lombok.Data;

@Data
public class EventImpl implements Event {
    String projectName;
    String subsystem;
    String topic;
    String objectId;
    Object meta;
}

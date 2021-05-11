package com.dtolabs.rundeck.core.event;

import lombok.Data;

import java.util.Date;

@Data
public class EventQueryImpl implements EventQuery {
    String projectName;
    String subsystem;
    String topic;
    String objectId;
    Date dateFrom;
    Date dateTo;
    Integer maxResults = 20;
    Integer offset = 0;
    Long sequence;
    EventQueryType queryType = EventQueryType.SELECT;
}

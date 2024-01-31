package com.dtolabs.rundeck.core.event;

import lombok.Data;
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery;
import org.rundeck.app.data.model.v1.storedevent.StoredEventQueryType;

import java.util.Date;

@Data
public class EventQueryImpl implements StoredEventQuery {
    String projectName;
    String subsystem;
    String topic;
    String objectId;
    Date dateFrom;
    Date dateTo;
    Integer maxResults = 20;
    Integer offset = 0;
    Long sequence;
    StoredEventQueryType queryType = StoredEventQueryType.SELECT;
}

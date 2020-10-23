package com.dtolabs.rundeck.core.event;

import org.rundeck.app.spi.AppService;

import java.util.Date;
import java.util.List;

public interface EventStoreService extends AppService {
    void storeEvent(Event event);

    void storeEventBatch(List<Event> events);

    EventQueryResult query(EventQuery query);
}

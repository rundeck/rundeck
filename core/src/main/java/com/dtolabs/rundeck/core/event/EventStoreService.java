package com.dtolabs.rundeck.core.event;

import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery;
import org.rundeck.app.spi.AppService;

import java.util.List;

public interface EventStoreService extends AppService {
    void storeEvent(Event event);

    void storeEventBatch(List<Event> events);

    EventQueryResult query(StoredEventQuery query);

    EventStoreService scoped(Event eventTemplate, StoredEventQuery queryTemplate);
}

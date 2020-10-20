package com.dtolabs.rundeck.core.event;

import org.rundeck.app.spi.AppService;

public interface EventStoreService extends AppService {
    void storeEvent(Event event, boolean transactional);

    EventQueryResult findEvents(EventQuery query);
}

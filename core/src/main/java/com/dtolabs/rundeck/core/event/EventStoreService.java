package com.dtolabs.rundeck.core.event;

import org.rundeck.app.spi.AppService;

import java.util.Date;
import java.util.List;

public interface EventStoreService extends AppService {
    void storeEvent(Event event, boolean transactional);

    void storeEventBatch(List<Event> events, boolean transactional);

    long removeBefore(Date date);

    long removeBetween(Date fromDate, Date toDate);

    EventQueryResult findEvents(EventQuery query);
}

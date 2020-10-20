package com.dtolabs.rundeck.core.event;

import java.util.List;

public interface EventQueryResult {
    Integer getTotalCount();
    List<Event> getEvents();
}

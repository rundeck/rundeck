package com.dtolabs.rundeck.core.event;

import java.util.List;

public interface EventQueryResult {
    Long getTotalCount();
    List<? extends Event> getEvents();
}

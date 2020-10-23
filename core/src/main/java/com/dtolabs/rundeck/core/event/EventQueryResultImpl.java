package com.dtolabs.rundeck.core.event;

import lombok.Data;

import java.util.List;

@Data
public class EventQueryResultImpl implements EventQueryResult {
    Long totalCount;
    List<? extends Event> events;
}

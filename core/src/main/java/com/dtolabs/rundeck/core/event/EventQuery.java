package com.dtolabs.rundeck.core.event;

import java.util.Date;

public interface EventQuery {
    String getProjectName();
    String getSubsystem();
    String getTopic();
    String getObjectId();
    Date getDateFrom();
    Date getDateTo();
    Integer getMaxResults();
    Integer getOffset();
    EventQueryType getQueryType();
}

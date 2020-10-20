package com.dtolabs.rundeck.core.event;

import java.util.Date;

public interface EventQuery extends Event {
    Date getDateFrom();
    Date getDateTo();
    Integer getMaxResults();
    Integer getOffset();
}

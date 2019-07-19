package com.dtolabs.rundeck.core.jobs;

public interface JobEventStatus {

    public default boolean isSuccessful() {
        return true;
    }

    public default String getDescription() {
        return null;
    }

    public default boolean useNewValues(){ return false; }

}

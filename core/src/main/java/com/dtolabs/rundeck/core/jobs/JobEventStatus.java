package com.dtolabs.rundeck.core.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface JobEventStatus {

    default boolean isSuccessful() {
        return true;
    }

    default String getDescription() {
        return null;
    }

    default boolean useNewValues(){ return false; }

    default Map getOptionsValues(){ return null; }

    default List<JobOption> getOptions(){ return new ArrayList<JobOption>(); }

}

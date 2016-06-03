package com.dtolabs.rundeck.core.dispatcher;

/**
 * Created by greg on 5/26/16.
 */
public interface MutableDataContext extends DataContext, Mergable<DataContext> {
    void merge(DataContext item);
}

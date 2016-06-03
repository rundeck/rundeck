package com.dtolabs.rundeck.core.dispatcher;

/**
 * Created by greg on 6/3/16.
 */
public interface Mergable<T> {
    void merge(T input);
}

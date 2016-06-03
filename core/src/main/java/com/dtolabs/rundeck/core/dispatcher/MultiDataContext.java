package com.dtolabs.rundeck.core.dispatcher;

import java.util.Map;

/**
 * Keyed data with optional base data set
 */
public interface MultiDataContext<K, D> {
    /**
     * @return base data set, or null
     */
    D getBase();

    /**
     * @return keyed data
     */
    Map<K, D> getData();

    /**
     * @param key key
     *
     * @return data for given key
     */
    D getData(K key);
}

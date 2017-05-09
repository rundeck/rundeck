package com.dtolabs.rundeck.core.dispatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 5/26/16.
 */
public interface MutableDataContext extends DataContext, Mergable<DataContext> {
    void merge(DataContext item);

    /**
     * Put value
     *
     * @param group
     * @param key
     * @param value
     *
     * @return
     */
    public default String put(String group, String key, String value) {
        return group(group).put(key, value);
    }

    default Map<String, String> group(final String group) {
        if (null == get(group)) {
            put(group, new HashMap<>());
        }
        return get(group);
    }
}

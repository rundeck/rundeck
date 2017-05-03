package com.dtolabs.rundeck.core.dispatcher;

import java.util.Map;
import java.util.Set;

/**
 * Keyed data with optional base data set
 */
public interface MultiDataContext<K extends ViewTraverse<K>, D extends DataContext> {
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

    Set<K> getKeys();


    /**
     * Merge the data into the key
     *
     * @param key
     * @param data
     */
    void merge(K key, D data);

    /**
     * Merge another multi context
     *
     * @param input
     */
    void merge(MultiDataContext<K, D> input);

    default String resolve(
            final K view,
            final String group,
            final String key
    )
    {
        return resolve(view, group, key, null);
    }

    default String resolve(
            final K view,
            final String group,
            final String key,
            final String defaultValue
    )
    {
        DataContext data = getBase();
        if (null != view) {
            K newview = view;
            data = getData(newview);
            while (null == data && !newview.isFinal()) {
                newview = newview.widenView().getView();
                data = getData(newview);
            }
            if (null == data) {
                data = getBase();
            }
        }
        return data != null ? data.resolve(group, key, defaultValue) : null;
    }
}

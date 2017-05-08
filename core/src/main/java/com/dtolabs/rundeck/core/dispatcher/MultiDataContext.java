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
        if (null != view) {
            DataContext data = getData(view);

            //expand view if no data at this scope
            if (null != data) {
                String result = data.resolve(group, key, null);

                if (null != result) {
                    return result;
                }
            }
            if (!view.isWidest()) {
                return resolve(view.widenView().getView(), group, key, defaultValue);
            }
            return resolve(null, group, key, defaultValue);
        }
        DataContext data = getBase();
        return data != null ? data.resolve(group, key, defaultValue) : defaultValue;
    }
}

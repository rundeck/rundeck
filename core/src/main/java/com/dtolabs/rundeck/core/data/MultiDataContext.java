/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.data;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Keyed data with optional base data set
 */
public interface MultiDataContext<K extends ViewTraverse<K>, D extends DataContext> {
    /**
     * @return base data set, or null
     */
    MultiDataContext<K, D> getBase();

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
    Set<K> getConsolidatedKeys();

    MultiDataContext<K, D> consolidate();


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

    /**
     * Resolve a data value from a starting scope
     *
     * @param view  scope
     * @param group group
     * @param key   key
     *
     * @return value or null
     */
    default String resolve(
            final K view,
            final String group,
            final String key
    )
    {
        return resolve(view, group, key, null);
    }

    /**
     * Resolve a data value from a starting scope or return a default
     *
     * @param view         scope
     * @param group        group
     * @param key          key
     * @param defaultValue default value
     *
     * @return resolved value, or the default value
     */
    default String resolve(
            final K view,
            final String group,
            final String key,
            final String defaultValue
    )
    {
        return resolve(view, null, group, key, defaultValue);
    }

    /**
     * Resolve a data value with optional scope widening or return a default value.
     * If no local value is found for the current scope,
     * the base data set will be queried for the current scope, and
     * any base value will be returned, before widening the search scope.
     *
     * @param view         scope
     * @param widestScope       maximum search scope
     * @param group        group
     * @param key          key
     * @param defaultValue default value
     *
     * @return resolved value or default value
     */
    default String resolve(
            final K view,
            final K widestScope,
            final String group,
            final String key,
            final String defaultValue
    )
    {
        if (null == view) {
            throw new NullPointerException("view is null");
        }
        DataContext data = getData(view);

        //expand view if no data at this scope
        if (null != data) {
            String result = data.resolve(group, key, null);

            if (null != result) {
                return result;
            }
        }
        MultiDataContext<K, D> base = getBase();
        if (base != null) {
            String resolve = base.resolve(view, view, group, key, null);
            if (null != resolve) {
                return resolve;
            }
        }
        if (null != widestScope && widestScope.isWider(view.widenView().getView())) {
            return defaultValue;
        }
        if (!view.isWidest()) {
            return resolve(view.widenView().getView(), widestScope, group, key, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Resolve all data values for the matching views
     *
     * @param viewFilter view filter
     * @param group      group
     * @param key        key
     *
     * @return List of all resolved values, or null
     */
    default List<String> collect(
            final Predicate<K> viewFilter,
            final String group,
            final String key
    )
    {
        return new TreeSet<>(getConsolidatedKeys())
                .stream()
                .filter(viewFilter)
                .map(v -> Optional.ofNullable(resolve(v, group, key)))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());
    }

    /**
     * Resolve all data values for the matching views
     *
     * @param viewFilter view filter
     * @param group      group
     * @param key        key
     *
     * @return List of all resolved values, or null
     */
    default Map<String, String> collectMap(
            final Predicate<K> viewFilter,
            final Function<K, String> keyMapper,
            final String group,
            final String key
    )
    {
        return new TreeSet<>(getConsolidatedKeys())
                .stream()
                .filter(viewFilter)
                .filter(v -> null != resolve(v, group, key))
                .collect(Collectors.toMap(keyMapper, v -> resolve(v, group, key)));
    }
}

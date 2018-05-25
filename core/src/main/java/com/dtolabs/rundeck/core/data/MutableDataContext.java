/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

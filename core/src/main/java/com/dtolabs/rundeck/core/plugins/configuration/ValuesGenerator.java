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

package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Generates dynamic values for a plugin property
 */
public interface ValuesGenerator {
    /**
     * @return a simple string list for the values
     */
    default List<String> generateValuesStrings() {
        return null;
    }

    /**
     * @return a list of key/label entries
     */
    default List<Entry> generateValues() {
        return null;
    }

    /**
     * Represents a key/label pair
     */
    interface Entry {
        /**
         * @return the key
         */
        String getKey();

        /**
         * @return display label
         */
        String getLabel();
    }

    static Entry entry(final String key, final String value) {
        return new Entry() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getLabel() {
                return value;
            }
        };
    }
}

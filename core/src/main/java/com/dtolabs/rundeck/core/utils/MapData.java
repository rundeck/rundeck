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

package com.dtolabs.rundeck.core.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Utility for traversing maps and extracting typed data
 */
public class MapData {
    /**
     * @param data         data
     * @param property     property
     * @param defaultValue default value
     * @return get property by name as boolean
     */
    public static boolean metaBooleanProp(final Map<String, Object> data, final String property, boolean defaultValue) {
        return metaPathBoolean(data, property, defaultValue);
    }

    /**
     * @param o object
     * @return true if the value is Boolean.TRUE or the string "true"
     */
    private static boolean asBoolean(final Object o) {
        if (o == Boolean.TRUE) {
            return true;
        }
        return "true".equals(o);
    }

    /**
     * @param data data
     * @param path dot "." separated path string
     * @return optional entry at the path
     */
    private static Optional<Object> metaPath(final Map<String, Object> data, final String path) {
        String[] parts = path.split("\\.", 2);
        Object object;
        if (parts.length > 0) {
            object = data.get(parts[0]);
        } else {
            return Optional.empty();
        }
        if (parts.length > 1) {
            if (object instanceof Map) {
                Map<String, Object> meta2 = (Map<String, Object>) object;
                return metaPath(meta2, parts[1]);
            } else {
                return Optional.empty();
            }
        }
        return Optional.ofNullable(object);
    }

    /**
     * @param data         data
     * @param path         dot "." separated path string
     * @param defaultValue
     * @return boolean found at the path
     */
    public static boolean metaPathBoolean(final Map<String, Object> data, final String path, boolean defaultValue) {
        return metaPathValue(data, path, defaultValue, MapData::asBoolean);
    }

    /**
     * @param data         data
     * @param path         dot "." separated path string
     * @param defaultValue default value if path is not found or not the right type
     * @return String found at the path, or defaultValue
     */
    public static String metaPathString(final Map<String, Object> data, final String path, String defaultValue) {
        return metaPathValue(data, path, defaultValue, MapData::asString);
    }

    /**
     * @param data         data
     * @param path         dot "." separated path string
     * @param defaultValue default value if path is not found or not the right type
     * @param <T>          result type
     * @param toValue      function to convert non-null object to expected type
     * @return String found at the path, or defaultValue
     */
    public static <T> T metaPathValue(
        final Map<String, Object> data,
        final String path,
        T defaultValue,
        Function<Object, T> toValue
    )
    {
        Optional<Object> o = metaPath(data, path);
        return o.map(toValue).orElse(defaultValue);
    }

    /**
     * @param data     data
     * @param property property
     * @return String found at the prop, or null
     */
    public static String metaStringProp(final Map<String, Object> data, final String property) {
        return metaStringProp(data, property, null);
    }

    /**
     * @param data         data
     * @param property     property
     * @param defaultValue default return value
     * @return String found at the prop, or default value
     */
    public static String metaStringProp(
        final Map<String, Object> data,
        final String property,
        final String defaultValue
    )
    {
        return metaPathString(data, property, defaultValue);
    }

    private static String asString(final Object obj) {
        return obj instanceof String ? (String) obj : null;
    }

    /**
     * @param obj input object
     * @return Integer value for the object, or null
     */
    public static Integer asInt(final Object obj) {
        try {
            return obj != null ? obj instanceof Integer
                                 ? (Integer) obj
                                 : obj instanceof String ? Integer.parseInt((String) obj) : null : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @param obj input object
     * @return Long value for the object, or null
     */
    public static Long asLong(final Object obj) {
        try {
            return obj != null ? obj instanceof Long
                                 ? (Long) obj
                                 : obj instanceof String ? Long.parseLong((String) obj) : null : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Convert all values to string via toString
     *
     * @param input
     */
    public static Map<String, String> toStringStringMap(Map input) {
        Map<String, String> map = new HashMap<>();
        for (Object o : input.keySet()) {
            map.put(o.toString(), input.get(o) != null ? input.get(o).toString() : "");
        }
        return map;
    }
}

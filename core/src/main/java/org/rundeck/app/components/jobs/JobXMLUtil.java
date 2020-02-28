/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components.jobs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility methods and data for use by Job XML XMAP de/serialization
 */
public class JobXMLUtil {
    /**
     * Prefix for map keys which will be converted to xml attributes
     */
    public static final String ATTR_PREFIX = "@attr:";
    /**
     * suffix for map keys of Collection values, to convert a collection to a plural/single* element set
     */
    public static final String PLURAL_SUFFIX = "[s]";
    public static final String PLURAL_REPL = "s";
    /**
     * Suffix for map key of string values, to output the content as CDATA section
     */
    public static final String CDATA_SUFFIX = "<cdata>";
    /**
     * Suffix for map key of map value, to output value map in "generic data" structure, allowing automatic reversal
     */
    public static final String DATAVALUE_SUFFIX = "<dataval>";
    /**
     * map key for text value to output the value as element text, must be the only non-attribute map entry on output.
     */
    public static final String TEXT_KEY = "<text>";

    /**
     * Add entry to the map for the given key, converting the key into an attribute key identifier
     */
    public static void addAttribute(Map map, String key, Object val) {
        map.put(asAttributeName(key), val);
    }

    /**
     * Return the key as an attribute key identifier
     */
    public static String asAttributeName(String key) {
        return ATTR_PREFIX + key;
    }

    /**
     * Replace the key in the map with the attribute key identifier, if the map entry exists and is not null
     */
    public static void makeAttribute(Map map, String key) {
        if (null != map) {
            final Object remove = map.remove(key);
            if (null != remove) {
                map.put(asAttributeName(key), remove);
            }
        }
    }


    /**
     * Return a Map with an attribute key identifier created from the given key, and the given value
     */
    public static Map toAttrMap(String key, Object val) {
        Map map = new LinkedHashMap();
        if (null != key) {
            map.put(asAttributeName(key), val);
        }
        return map;
    }

    /**
     * Return the pluralized key form of the key
     */
    public static String pluralize(String key) {
        if (key.endsWith(PLURAL_SUFFIX)) {
            return key;
        } else if (key.endsWith(PLURAL_REPL)) {
            String k = key.substring(0, key.length() - PLURAL_REPL.length());
            return k + PLURAL_SUFFIX;
        }
        return key + PLURAL_SUFFIX;
    }

    /**
     * change the key for the map entry to the pluralized key form
     */
    public static Map makePlural(Map map, String key) {
        map.put(pluralize(key), map.remove(key));
        return map;
    }

    /**
     * Return the key name for use as a CDATA section
     */
    public static String asCDATAName(String key) {
        return key + CDATA_SUFFIX;
    }

    /**
     * Return the key name for use as generic data structure
     */
    public static String asDataValueKey(String key) {
        return key + DATAVALUE_SUFFIX;
    }
}

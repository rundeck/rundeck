/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * a toString formatter utility. Takes a map of key value pairs that represent the fields
 * in an object and prints them in a formatted manner. Nested objects included in the map
 * will have their toString() method called.
 * <p/>
 * Example output on an instance of {@link com.dtolabs.rundeck.core.types.controller.ExecuteAction}:
 * ExecuteAction{user='rubble', failonerror='true', command='Command{executionDaemonized='false', name='aCommand'}',
 * context='Context{project='TestSshCommand', resourceType='TypeA', resourceName='aTypeInstance'}',
 * strategy='ant', adExecutable='ad', properties='[]'}
 */
public class ToStringFormatter {

    private final Map params;
    private final Object obj;

    ToStringFormatter(final Object obj, final Map params) {
        this.obj = obj;
        this.params = params;
    }

    /**
     * Factory method. Creates a formatter
     *
     * @param obj    Object to print toString message for
     * @param params map of key value pairs to print in message
     * @return new instance
     */
    public static ToStringFormatter create(final Object obj, final Map params) {
        return new ToStringFormatter(obj, params);
    }

    /**
     * Prints message. Defaults to filtering null values out of the output.
     *
     * @return output message
     */
    public String toString() {
        return toString(true);
    }

    /**
     * Prints message with contents of params map.  The name of the class does
     * not include it's package namespace.
     *
     * @param fiilterNulls if true, null values and their keys will not be included
     * @return output message
     */
    public String toString(final boolean fiilterNulls) {
        final StringBuffer sb = new StringBuffer();
        for (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
            final String key = (String) iter.next();
            final Object value = params.get(key);
            if (fiilterNulls && null == value) {
                continue;
            } else {
                appendKeyValue(key, value, sb);
            }
        }
        final String fullClassName = obj.getClass().getName();
        final String shortClassName = fullClassName.split("\\.")[fullClassName.split("\\.").length - 1];
        return shortClassName + "{" + sb.toString() + "}";
    }

    /**
     * Takes a key and value, places an equal sign character between them and single quotes around the value
     *
     * @param key   attribute name (lhs)
     * @param value value (rhs)
     * @param sb    string buffer that is being built up
     * @return sb that was passed in.
     */
    StringBuffer appendKeyValue(final String key, final Object value, final StringBuffer sb) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(key);
        sb.append("='");
        sb.append(value.toString());
        sb.append("'");
        return sb;
    }

}

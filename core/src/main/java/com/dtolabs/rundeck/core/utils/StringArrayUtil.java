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

import java.util.*;

/**
 * Utility for managing string arrays
 */
public class StringArrayUtil {

    /**
     * Merge to string arrays
     *
     * @param input Array elements to add
     * @param list  List to merge input into
     * @return string array of merged set
     */
    public static String[] merge(final String[] input, final String[] list) {
        final List<String> v = new ArrayList<String>(Arrays.asList(list));
        for (final String anInput : input) {
            if ((null != anInput) && !v.contains(anInput)) {
                v.add(anInput);
            }
        }
        return v.toArray(new String[v.size()]);
    }
    /**
     * Subtract one string array from another
     *
     * @param input Array elements to subtract
     * @param list  List to subtract from
     *
     * @return string array of merged set
     */
    public static String[] subtract(final String[] input, final String[] list) {
        final Set<String> difference = new HashSet<String>(Arrays.asList(list));
        difference.removeAll(Arrays.asList(input));
        return difference.toArray(new String[difference.size()]);
    }

    /**
     * Format an array of objects as a string separated by a delimiter by calling toString on each object
     *
     * @param input List to format
     * @param delim delimiter string to insert between elements
     * @return formatted string
     */
    public static String asString(final Object[] input, final String delim) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0 ; i < input.length ; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(input[i].toString());
        }
        return sb.toString();
    }
    /**
     * Format a string array
     *
     * @param input List to format
     * @param delim delimiter string to insert between elements
     * @return formatted string
     */
    public static String asString(final String[] input, final String delim) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            if (i > 0) { sb.append(delim); }
            sb.append(input[i]);
        }
        return sb.toString();
    }

    /**
     * The difference set operation
     *
     * @param list1 list1
     * @param list2 list2
     * @return the set of all items not in both lists
     */
    public static String[] difference(final String[] list1, final String[] list2) {
        HashSet<String> set = new HashSet<String>();
        HashSet<String> set1 = new HashSet<String>(Arrays.asList(list1));
        HashSet<String> set2 = new HashSet<String>(Arrays.asList(list2));
        for (final String s : list1) {
            if (!set2.contains(s)) {
                set.add(s);
            }
        }
        for (final String s : list2) {
            if (!set1.contains(s)) {
                set.add(s);
            }
        }
        return set.toArray(new String[set.size()]);
    }

    /**
     * @return true if the value is in the list.
     * @param list list
     * @param value value
     */
    public static boolean contains(final String[] list, final String value){
        HashSet<String> set = new HashSet<String>(Arrays.asList(list));
        return set.contains(value);
    }

}

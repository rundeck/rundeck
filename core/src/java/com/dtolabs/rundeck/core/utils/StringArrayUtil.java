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
        final List v = new ArrayList(Arrays.asList(list));
        for (int i = 0; i < input.length; i++) {
            if ((null != input[i]) && !v.contains(input[i])) {
                v.add(input[i]);
            }
        }
        return (String[]) v.toArray(new String[0]);
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
        final Set difference = new HashSet(Arrays.asList(list));
        difference.removeAll(Arrays.asList(input));
        return (String[]) difference.toArray(new String[difference.size()]);
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
            sb.append(input[i]);
            if (i < input.length - 1) sb.append(delim);
        }
        return sb.toString();
    }

    /**
     * The difference set operation
     *
     * @param list1
     * @param list2
     * @return the set of all items not in both lists
     */
    public static String[] difference(final String[] list1, final String[] list2) {
        HashSet set = new HashSet();
        HashSet set1 = new HashSet(Arrays.asList(list1));
        HashSet set2 = new HashSet(Arrays.asList(list2));
        for (int i = 0; i < list1.length; i++) {
            String s = list1[i];
            if(!set2.contains(s)){
                set.add(s);
            }
        }
        for (int i = 0; i < list2.length; i++) {
            String s = list2[i];
            if (!set1.contains(s)) {
                set.add(s);
            }
        }
        return (String[]) set.toArray(new String[0]);
    }

    /**
     * Returns true if the value is in the list.
     * @param list
     * @param value
     * @return
     */
    public static boolean contains(final String[] list, final String value){
        HashSet set = new HashSet(Arrays.asList(list));
        return set.contains(value);
    }

}

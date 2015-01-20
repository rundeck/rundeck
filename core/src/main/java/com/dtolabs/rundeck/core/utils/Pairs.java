/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for using {@link Pair} instances
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-06-26
 */
public class Pairs {
    /**
     * Return a List of the first items from a list of pairs
     * @param list list
     * @param <T> first type
     * @param <W> second type
     * @return list of firsts
     */
    public static <T,W> List<T> listFirst(List<Pair<T, W>> list) {
        ArrayList<T> ts = new ArrayList<T>();
        for (Pair<T, W> twPair : list) {
            ts.add(twPair.getFirst());
        }
        return ts;
    }

    /**
     * Return a List of the second items from a list of pairs
     * @param list list
     * @param <T> first type
     * @param <W> second type
     * @return list of seconds
     */
    public static <T,W> List<W> listSecond(List<Pair<T, W>> list) {
        ArrayList<W> ts = new ArrayList<W>();
        for (Pair<T, W> twPair : list) {
            ts.add(twPair.getSecond());
        }
        return ts;
    }
}

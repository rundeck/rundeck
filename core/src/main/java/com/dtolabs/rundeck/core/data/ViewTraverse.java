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

/**
 * Allows a contextual view to produce a widened view
 *
 * @author greg
 * @since 5/2/17
 */
public interface ViewTraverse<T> {
    default boolean isWidest() {
        return true;
    }

    default boolean globExpandTo(T x) {
        return true;
    }

    default ViewTraverse<T> widenView() {
        return null;
    }
    default ViewTraverse<T> merge(T source) {
        return null;
    }

    /**
     * Return true if the input is wider than the current view
     * @param source
     * @return
     */
    default boolean isWider(T source){
        return false;
    }

    default T getView() {
        return null;
    }
}

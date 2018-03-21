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

import java.util.function.BiFunction;

/**
 * Expand a context variable
 *
 * @author greg
 * @since 5/31/17
 */
public interface VarExpander {
    /**
     * Expand a variable
     *
     * @param data        multi context data
     * @param viewMap     create a view
     * @param variableref reference text
     * @param <T>         view type
     *
     * @return expanded value, or null if not available or cannot be determined
     */
    <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final String variableref
    );
}

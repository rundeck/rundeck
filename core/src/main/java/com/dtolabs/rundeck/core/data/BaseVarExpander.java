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

import lombok.Data;

import java.util.function.BiFunction;

/**
 * @author greg
 * @since 5/31/17
 */
public abstract class BaseVarExpander implements VarExpander {
    @Override
    public <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final BiFunction<Integer, String, T> viewMap,
            final String variableref
    )
    {

        VariableRef variableRef = parseVariable(variableref);
        if (null == variableRef) {
            return null;
        }
        String step = variableRef.getStep();
        String group = variableRef.getGroup();
        String key = variableRef.getKey();
        String qual = variableRef.getNode();
        return SharedDataContextUtils.expandVariable(data, viewMap, step, group, key, qual);

    }

    /**
     * Parse a string defining a variable reference into a VariableRef object
     *
     * @param variableref string
     *
     * @return new ref
     */
    protected abstract VariableRef parseVariable(final String variableref);

    /**
     * A reference to a scoped context variable
     *
     * @author greg
     * @since 5/31/17
     */
    @Data static class VariableRef {
        private final String variableref;
        private final String step;
        private final String group;
        private final String key;
        private final String node;
    }
}

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

import java.util.List;
import java.util.function.BiFunction;

/**
 * @author greg
 * @since 5/31/17
 */
public abstract class BaseVarExpander implements VarExpander {
    @Override
    public <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final T currentContext,
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
        Boolean glob = variableRef.getNodeglob();
        String format = variableRef.getFormat();
        if (glob) {
            List<String> strings = expandAllNodesVariable(data, currentContext, viewMap, step, group, key);
            if (strings == null) {
                return null;
            }
            //todo:FORMAT
            String delimiter = ",";
            if (format != null && format.length() == 1) {
                delimiter = format;
            }
            return String.join(delimiter, strings);
        } else {
            return expandVariable(data, currentContext, viewMap, step, group, key, qual);
        }
    }



    /**
     * Expand a variable reference
     *
     * @param data    multi context data
     * @param viewMap factory of ViewTraverse type
     * @param step    step text
     * @param group
     * @param key
     * @param node
     * @param <T>
     *
     * @return
     */
    public static <T extends ViewTraverse<T>> String expandVariable(
            final MultiDataContext<T, DataContext> data,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final String step,
            final String group,
            final String key,
            final String node
    )
    {
        Integer t = null;
        if (null != step) {
            try {
                t = Integer.parseInt(step);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        T view = viewMap.apply(t, node);
        T mergedview = view.merge(currentContext).getView();
        return data.resolve(mergedview, view, group, key, null);
    }

    /**
     * Expand a variable reference
     *
     * @param data  multi context data
     * @param step  step text
     * @param group
     * @param key
     * @param <T>
     *
     * @return list of values, or empty list
     */
    public static <T extends ViewTraverse<T>> List<String> expandAllNodesVariable(
            final MultiDataContext<T, DataContext> data,
            final T currentContext,
            final BiFunction<Integer, String, T> viewMap,
            final String step,
            final String group,
            final String key
    )
    {
        final Integer t;
        if (null != step) {
            try {
                t = Integer.parseInt(step);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            t = null;
        }
        T containerView = viewMap.apply(t, null);
        T mergedview = containerView.merge(currentContext).getView();
        return data.collect(containerView::globExpandTo, group, key);
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
        private final Boolean nodeglob;
        private final String format;
    }
}

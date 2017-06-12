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

import lombok.ToString;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by greg on 6/3/16.
 */
@ToString
public class MultiDataContextImpl<K extends ViewTraverse<K>, D extends DataContext> implements MultiDataContext<K, D> {
    Map<K, D> map;
    private MultiDataContext<K, D> base;

    public MultiDataContextImpl(final Map<K, D> map) {
        this.map = map;
    }

    public MultiDataContextImpl() {
        this(new HashMap<>());
    }

    public MultiDataContextImpl(MultiDataContext<K, D> orig) {
        this(new HashMap<>());
        this.base = orig;
    }


    public static <K extends ViewTraverse<K>, D extends DataContext> MultiDataContextImpl<K, D> with(MultiDataContext<K, D> original) {
        return new MultiDataContextImpl<>(original);
    }

    @Override
    public Map<K, D> getData() {
        return map;
    }

    @Override
    public D getData(final K key) {
        return map.get(key);
    }

    @Override
    public Set<K> getKeys() {
        return map.keySet();
    }

    @Override
    public Set<K> getConsolidatedKeys() {
        HashSet<K> ks = new HashSet<>();
        if (getBase() != null) {
            ks.addAll(getBase().getConsolidatedKeys());
        }
        ks.addAll(getKeys());
        return ks;
    }

    @Override
    public MultiDataContext<K, D> getBase() {
        return base;
    }


    @Override
    public void merge(final K k, final D data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        if (map.containsKey(k)) {
            map.get(k).merge(data);
        } else {
            map.put(k, data);
        }
    }

    protected void setBase(MultiDataContext<K, D> base) {
        this.base = base;
    }

    public void merge(MultiDataContext<K, D> input) {
        if (null == input) {
            return;
        }
        //merge map and data
        for (K k : input.getData().keySet()) {
            D data = input.getData().get(k);
            if (null != data) {
                merge(k, data);
            }
        }
    }

    @Override
    public MultiDataContext<K, D> consolidate() {
        MultiDataContextImpl<K, D> consolidated = new MultiDataContextImpl<>();
        if (null != base) {
            consolidated.merge(base.consolidate());
        }
        consolidated.merge(this);
        return consolidated;
    }
}

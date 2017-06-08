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

import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MutableDataContext;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 5/25/16.
 */
public class BaseDataContext implements MutableDataContext {
    private Map<String, Map<String, String>> data;

    public BaseDataContext() {
        this(new HashMap<>());
    }
    public BaseDataContext(DataContext context) {
        this(new HashMap<>());
        merge(context);
    }

    public BaseDataContext(final Map<String, Map<String, String>> data) {
        if(null!=data) {
            this.data = data;
        }else{
            this.data = new HashMap<>();
        }
    }
    public BaseDataContext(String key, Map<String, String> value) {
        this.data = new HashMap<>();
        data.put(key, new HashMap<>(value));
    }
    public void merge(DataContext context){
        data= DataContextUtils.merge(data, context.getData());
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(final Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return data.containsValue(value);
    }

    @Override
    public Map<String, String> get(final Object key) {
        return data.get(key);
    }

    @Override
    public Map<String, String> put(final String key, final Map<String, String> value) {
        return data.put(key, value);
    }

    @Override
    public Map<String, String> remove(final Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends Map<String, String>> m) {
        data.putAll(m);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set<String> keySet() {
        return data.keySet();
    }

    @Override
    public Collection<Map<String, String>> values() {
        return data.values();
    }

    @Override
    public Set<Entry<String, Map<String, String>>> entrySet() {
        return data.entrySet();
    }

    @Override
    public Map<String, Map<String, String>> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "BaseDataContext{" +
                data +
               '}';
    }
}

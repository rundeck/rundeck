package com.dtolabs.rundeck.core.dispatcher;

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
        this(new HashMap<String, Map<String, String>>());
    }
    public BaseDataContext(DataContext context) {
        this(new HashMap<String, Map<String, String>>());
        merge(context);
    }

    public BaseDataContext(final Map<String, Map<String, String>> data) {
        if(null!=data) {
            this.data = data;
        }else{
            this.data = new HashMap<>();
        }
    }
    public void merge(DataContext context){
        data=DataContextUtils.merge(data, context.getData());
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
               "data=" + data +
               '}';
    }
}

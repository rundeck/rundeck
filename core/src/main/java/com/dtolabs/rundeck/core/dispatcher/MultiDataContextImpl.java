package com.dtolabs.rundeck.core.dispatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by greg on 6/3/16.
 */
public class MultiDataContextImpl<K, D extends Mergable<D>> implements MultiDataContext<K, D> {
    Map<K, D> map;
    private D base;

    /**
     * Has a base data set but not multi
     */
    public MultiDataContextImpl(final D base) {
        this.base = base;
    }

    public static <K, D extends Mergable<D>> MultiDataContextImpl<K, D> withBase(final D base) {
        MultiDataContextImpl<K, D> kdMultiDataContext = new MultiDataContextImpl<>();
        kdMultiDataContext.setBase(base);
        return kdMultiDataContext;
    }
    public static <K, D extends Mergable<D>> MultiDataContextImpl<K, D> with(MultiDataContext<K,D> original) {
        MultiDataContextImpl<K, D> kdMultiDataContext = new MultiDataContextImpl<>();
        kdMultiDataContext.setBase(original.getBase());
        kdMultiDataContext.getData().putAll(original.getData());
        return kdMultiDataContext;
    }

    public MultiDataContextImpl(final Map<K, D> map) {
        this.map = map;
    }

    public MultiDataContextImpl() {
        this(new HashMap<K, D>());
    }

    public MultiDataContextImpl(MultiDataContext<K, D> orig) {
        this(new HashMap<K, D>());
        merge(orig);
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
    public D getBase() {
        return base;
    }

    protected void setBase(D base) {
        this.base = base;
    }

    public void merge(MultiDataContext<K, D> input) {
        if (null == input) {
            return;
        }
        if (null != base && null != input.getBase()) {
            base.merge(input.getBase());
        }
        //merge map and data
        for (K k : input.getData().keySet()) {
            if (map.containsKey(k)) {
                map.get(k).merge(input.getData().get(k));
            } else {
                map.put(k, input.getData(k));
            }
        }
    }

    @Override
    public String toString() {
        return "com.dtolabs.rundeck.core.dispatcher.MultiDataContextImpl{" +
               "map=" + map +
               ", base=" + base +
               '}';
    }

}

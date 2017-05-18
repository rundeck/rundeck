package com.dtolabs.rundeck.core.dispatcher;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 6/3/16.
 */
@ToString
public class MultiDataContextImpl<K extends ViewTraverse<K>, D extends DataContext> implements MultiDataContext<K, D> {
    Map<K, D> map;
    private D base;

    /**
     * Has a base data set but not multi
     */
    public MultiDataContextImpl(final D base) {
        this();
        this.base = base;
    }

    public MultiDataContextImpl(final Map<K, D> map) {
        this.map = map;
    }

    public MultiDataContextImpl() {
        this(new HashMap<>());
    }

    public MultiDataContextImpl(MultiDataContext<K, D> orig) {
        this(new HashMap<>());
        merge(orig);
    }

    public static <K extends ViewTraverse<K>, D extends DataContext> MultiDataContextImpl<K, D> withBase(final D base) {
        MultiDataContextImpl<K, D> kdMultiDataContext = new MultiDataContextImpl<>();
        kdMultiDataContext.setBase(base);
        return kdMultiDataContext;

    }

    public static <K extends ViewTraverse<K>, D extends DataContext> MultiDataContextImpl<K, D> with(MultiDataContext<K, D> original) {
        MultiDataContextImpl<K, D> kdMultiDataContext = new MultiDataContextImpl<>(original.getBase());
        kdMultiDataContext.getData().putAll(original.getData());
        return kdMultiDataContext;
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
    public D getBase() {
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
            D data = input.getData().get(k);
            if (null != data) {
                merge(k, data);
            }
        }
    }
}

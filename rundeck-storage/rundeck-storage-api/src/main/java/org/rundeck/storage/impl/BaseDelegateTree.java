package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Tree;

/**
 * Abstract base for a Tree with a delegate
 */
public abstract class BaseDelegateTree<T extends ContentMeta> extends StringToPathTree<T> implements Tree<T> {
    private Tree<T> delegate;

    private BaseDelegateTree() {

    }

    public BaseDelegateTree(Tree<T> delegate) {
        this.delegate = delegate;
    }

    public Tree<T> getDelegate() {
        return delegate;
    }
}

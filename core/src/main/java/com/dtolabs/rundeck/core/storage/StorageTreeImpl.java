package com.dtolabs.rundeck.core.storage;

import org.rundeck.storage.api.Tree;
import org.rundeck.storage.impl.DelegateTree;

/**
 * StorageTree implementation using a delegate.
 *
 * @author greg
 * @since 2014-02-19
 */
public class StorageTreeImpl extends DelegateTree<ResourceMeta> implements StorageTree {
    public StorageTreeImpl(Tree<ResourceMeta> delegate) {
        super(delegate);
    }
}

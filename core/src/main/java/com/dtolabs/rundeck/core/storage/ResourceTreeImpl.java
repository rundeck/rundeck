package com.dtolabs.rundeck.core.storage;

import org.rundeck.storage.api.Tree;
import org.rundeck.storage.impl.DelegateTree;

/**
 * ResourceTree implementation using a delegate.
 *
 * @author greg
 * @since 2014-02-19
 */
public class ResourceTreeImpl extends DelegateTree<ResourceMeta> implements ResourceTree {
    public ResourceTreeImpl(Tree<ResourceMeta> delegate) {
        super(delegate);
    }
}

package com.dtolabs.rundeck.core.resourcetree;

import org.rundeck.storage.api.Tree;
import org.rundeck.storage.impl.DelegateTree;

/**
 * ResourceTreeImpl is ...
 *
 * @author greg
 * @since 2014-02-19
 */
public class ResourceTreeImpl extends DelegateTree<ResourceMeta> implements ResourceTree {
    public ResourceTreeImpl(Tree<ResourceMeta> delegate) {
        super(delegate);
    }
}

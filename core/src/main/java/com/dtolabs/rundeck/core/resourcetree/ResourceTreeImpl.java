package com.dtolabs.rundeck.core.resourcetree;

import us.vario.greg.lct.impl.DelegateTree;
import us.vario.greg.lct.model.Tree;

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

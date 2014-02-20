package org.rundeck.storage.conf;

import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathSelector;
import org.rundeck.storage.api.Tree;

/**
 * Delegates a tree and has a path selector
 */
public class DelegateSelectiveTree<T extends ContentMeta> extends DelegateTree<T> implements SelectiveTree<T> {
    PathSelector selector;

    public DelegateSelectiveTree(Tree<T> delegate, PathSelector selector) {
        super(delegate);
        this.selector = selector;
    }

    @Override
    public boolean matchesPath(Path path) {
        return selector.matchesPath(path);
    }
}

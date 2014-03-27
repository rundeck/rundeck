package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathSelector;
import org.rundeck.storage.api.Tree;

/**
 * A Tree that handles only a subset of paths
 */
public interface SelectiveTree<T extends ContentMeta> extends Tree<T>, SubPath {

}

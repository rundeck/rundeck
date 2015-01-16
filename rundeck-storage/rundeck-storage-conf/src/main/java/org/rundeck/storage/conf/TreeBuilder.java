package org.rundeck.storage.conf;

import org.rundeck.storage.impl.EmptyTree;
import org.rundeck.storage.api.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Resource Trees.  Allows extending a base tree with other trees at sub paths.  Content converters
 * and listeners can be
 * added selectively to sub paths, or based on analyzing the content.
 */
public class TreeBuilder<T extends ContentMeta> {
    Tree<T> base;
    List<SelectiveTree<T>> treeStack;

    private TreeBuilder() {
        treeStack = new ArrayList<SelectiveTree<T>>();
    }

    /**
     * Build a new tree with an empty base
     *
     * @param <T> content type
     *
     * @return builder
     */
    public static <T extends ContentMeta> TreeBuilder<T> builder() {
        return new TreeBuilder<T>().base(new EmptyTree<T>());
    }

    /**
     * Build a new tree with given base
     *
     * @param base base tree
     * @param <T>  content type
     *
     * @return builder
     */
    public static <T extends ContentMeta> TreeBuilder<T> builder(Tree<T> base) {
        return new TreeBuilder<T>().base(base);
    }

    /**
     * Set the base tree to be extended
     *
     * @param base base tree
     *
     * @return builder
     */
    public TreeBuilder<T> base(Tree<T> base) {
        this.base = base;
        return this;
    }

    /**
     * Add a tree responsible for a subpath of the base tree.
     *
     * @param path     sub path to handle
     * @param subtree  tree to delegate to for the path
     * @param fullPath if true, send resources to the subtree with the original full path, otherwise store with a path
     *                 relative to the path
     *
     * @return builder
     */
    public TreeBuilder<T> subTree(Path path, Tree<T> subtree, boolean fullPath) {
        treeStack.add(new SubPathTree<T>(subtree, path, fullPath));
        return this;
    }

    /**
     * Convert data content for all resources below the given path
     *
     * @param converter content converter
     * @param path      sub path to convert data
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter, Path path) {
        return convert(converter, PathUtil.subpathSelector(path));
    }

    /**
     * Convert data content for all resource paths matched by the path selector
     *
     * @param converter content converter
     * @param selector  path selection
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter, PathSelector selector) {
        return TreeBuilder.<T>builder(new ConverterTree<T>(build(), converter, selector));
    }

    /**
     * Convert data content for all resources matching the selector
     *
     * @param converter content converter
     * @param selector  resource selector
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter, ResourceSelector<T> selector) {
        return TreeBuilder.<T>builder(new ConverterTree<T>(build(), converter, selector));
    }

    /**
     * Convert data content for all resources matching the resource selector and within the sub path
     *
     * @param converter        content converter
     * @param subpath          sub path
     * @param resourceSelector resource selector
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter, Path subpath,
            ResourceSelector<T> resourceSelector) {
        return convert(converter, PathUtil.subpathSelector(subpath), resourceSelector);
    }

    /**
     * Convert data content for all resources matching the resource selector and the path selector
     *
     * @param converter        content converter
     * @param pathSelector     path selector
     * @param resourceSelector resource selector
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter, PathSelector pathSelector,
            ResourceSelector<T> resourceSelector) {
        return TreeBuilder.<T>builder(new ConverterTree<T>(build(), converter, pathSelector, resourceSelector));
    }

    /**
     * Convert all content in the tree
     *
     * @param converter converter
     *
     * @return builder
     */
    public TreeBuilder<T> convert(ContentConverter<T> converter) {
        return TreeBuilder.<T>builder(new ConverterTree<T>(build(), converter, PathUtil.allpathSelector()));
    }

    /**
     * Listen to events on all paths of the tree
     *
     * @param listener listener
     *
     * @return builder
     */
    public TreeBuilder<T> listen(Listener<T> listener) {
        return listen(listener, PathUtil.allpathSelector());
    }

    /**
     * Listen to events on selective paths of the tree
     *
     * @param listener listener
     * @param pathSelector path selector
     * @return builder
     */
    public TreeBuilder<T> listen(Listener<T> listener, PathSelector pathSelector) {
        return TreeBuilder.<T>builder(new ListenerTree<T>(build(), listener, pathSelector));
    }
    /**
     * Listen to events on selective paths of the tree
     *
     * @param listener listener
     * @param subpath sub path
     * @return builder
     */
    public TreeBuilder<T> listen(Listener<T> listener, Path subpath) {
        return TreeBuilder.<T>builder(new ListenerTree<T>(build(), listener, PathUtil.subpathSelector(subpath)));
    }

    /**
     * Listen to events on selective resources of the tree
     *
     * @param listener listener
     * @param resourceSelector resource selector
     * @return builder
     */
    private TreeBuilder<T> listen(Listener<T> listener, ResourceSelector<T> resourceSelector) {
        return TreeBuilder.<T>builder(new ListenerTree<T>(build(), listener, resourceSelector));
    }
    /**
     * Listen to events on selective resources of the tree
     *
     * @param listener listener
     * @param resourceSelector resource selector
     * @return builder
     */
    private TreeBuilder<T> listen(Listener<T> listener, String resourceSelector) {
        return TreeBuilder.<T>builder(new ListenerTree<T>(build(), listener,
                PathUtil.<T>resourceSelector(resourceSelector)));
    }

    /**
     * Listen to events on selective resources and paths of the tree
     *
     * @param listener         listener
     * @param pathSelector     path selector
     * @param resourceSelector resource selector
     *
     * @return builder
     */
    private TreeBuilder<T> listen(Listener<T> listener, PathSelector pathSelector,
            ResourceSelector<T> resourceSelector) {
        return TreeBuilder.<T>builder(new ListenerTree<T>(build(), listener,
                pathSelector, resourceSelector));
    }

    /**
     * Build the tree
     *
     * @return the tree
     */
    public Tree<T> build() {
        Tree<T> result = base;
        if (null == base && treeStack.size() == 1) {
            result = treeStack.get(0);
        } else if (treeStack.size() > 0) {
            result = new TreeStack<T>(treeStack, base);
        }else if(null==base) {
            throw new IllegalArgumentException("base tree was not set");
        }

        return result;
    }
}

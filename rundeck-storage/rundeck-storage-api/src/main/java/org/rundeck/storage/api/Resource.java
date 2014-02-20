package org.rundeck.storage.api;

/**
 * A resource in a tree
 */
public interface Resource<T extends ContentMeta> extends PathItem {
    public T getContents();

    boolean isDirectory();
}

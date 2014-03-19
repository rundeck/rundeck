package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

/**
 * Delegate pattern for resource
 */
public class DelegateResource<T extends ContentMeta> implements Resource<T> {
    Resource<T> delegate;

    public DelegateResource(Resource<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Path getPath() {
        return delegate.getPath();
    }

    @Override
    public T getContents() {
        return delegate.getContents();
    }

    @Override
    public boolean isDirectory() {
        return delegate.isDirectory();
    }
}

package org.rundeck.storage.impl;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

/**
 * $INTERFACE is ... User: greg Date: 2/14/14 Time: 12:51 PM
 */
public class ResourceBase<T extends ContentMeta> extends PathItemBase implements Resource<T> {
    private T contents;
    boolean directory;

    public ResourceBase(Path path, T contents, boolean directory) {
        super(path);
        this.setContents(contents);
        this.directory = directory;
    }

    @Override
    public T getContents() {
        return contents;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    protected void setContents(T contents) {
        this.contents = contents;
    }
}

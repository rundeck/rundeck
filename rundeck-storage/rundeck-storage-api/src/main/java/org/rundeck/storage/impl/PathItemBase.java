package org.rundeck.storage.impl;

import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathItem;

/**
 * $INTERFACE is ... User: greg Date: 2/14/14 Time: 12:55 PM
 */
public abstract class PathItemBase implements PathItem {

    private Path path;

    public PathItemBase(Path path) {
        this.path = path;
    }

    @Override
    public Path getPath() {
        return path;
    }

}

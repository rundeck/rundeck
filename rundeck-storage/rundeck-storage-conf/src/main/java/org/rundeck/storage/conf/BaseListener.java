package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.Resource;

import java.util.Set;

/**
 * BaseListener provides noop listener implementation.
 *
 * @author greg
 * @since 2014-03-14
 */
public class BaseListener<T extends ContentMeta> implements Listener<T> {
    @Override
    public void didGetPath(Path path, Resource<T> resource) {
    }

    @Override
    public void didGetResource(Path path, Resource<T> resource) {
    }

    @Override
    public void didListDirectoryResources(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didListDirectory(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didListDirectorySubdirs(Path path, Set<Resource<T>> contents) {
    }

    @Override
    public void didDeleteResource(Path path, boolean success) {
    }

    @Override
    public void didCreateResource(Path path, T content, Resource<T> contents) {
    }

    @Override
    public void didUpdateResource(Path path, T content, Resource<T> contents) {
    }
}

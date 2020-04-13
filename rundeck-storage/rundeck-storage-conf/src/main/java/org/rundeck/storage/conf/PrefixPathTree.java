package org.rundeck.storage.conf;

import org.rundeck.storage.api.ContentMeta;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.api.Tree;

/**
 * SelectiveTree that Maps resources into a delegate, that appends a path prefix to requests.
 *
 * This provides a root tree mapped to a subpath in the delegate.
 *
 * <ul>
 *     <li>Input Path: <pre>a/b</pre></li>
 *     <li>Path used for delegate: <pre>${pathPrefix}/a/b</pre></li>
 * </ul>
 * @param <T>
 */
public class PrefixPathTree<T extends ContentMeta>
        extends SubPathTree<T>
        implements SelectiveTree<T>
{

    public PrefixPathTree(final Tree<T> delegate, final String pathPrefix) {
        super(delegate, pathPrefix, false);
    }

    public PrefixPathTree(
            final Tree<T> delegate,
            final Path rootPath
    )
    {
        super(delegate, rootPath, false);
    }

    @Override
    protected boolean isLocalRoot(final Path path) {
        return PathUtil.isRoot(path);
    }

    @Override
    protected String translatePathInternal(final String extpath) {
        return PathUtil.appendPath(rootPath.getPath(), extpath);
    }

    @Override
    protected String translatePathExternal(final String intpath) {
        return PathUtil.removePrefix(rootPath.getPath(), intpath);
    }
}

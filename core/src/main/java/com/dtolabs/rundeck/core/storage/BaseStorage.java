package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.data.DataUtil;

import java.io.InputStream;
import java.util.Map;

public abstract class BaseStorage {

    Object hasPath(AuthContext context, String path) {
        return getServiceTree().hasPath(context, PathUtil.asPath(path));
    }

    Object hasResource(AuthContext context, String path) {
        return getServiceTree().hasResource(context, PathUtil.asPath(path));
    }

    Object getResource(AuthContext context, String path) {
        return getServiceTree().getPath(context, PathUtil.asPath(path));
    }

    Object updateResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        return getServiceTree().updateResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta,
                StorageUtil.factory()));
    }
    Object createResource(AuthContext context, String path, Map<String, String> meta, InputStream data) {
        return getServiceTree().createResource(context, PathUtil.asPath(path), DataUtil.withStream(data, meta, StorageUtil.factory()));
    }

    Object listDir(AuthContext context, String path) {
        return getServiceTree().listDirectory(context, PathUtil.asPath(path));
    }

    Object delResource(AuthContext context, String path) {
        return getServiceTree().deleteResource(context, PathUtil.asPath(path));
    }

    protected abstract AuthStorageTree getServiceTree();
}

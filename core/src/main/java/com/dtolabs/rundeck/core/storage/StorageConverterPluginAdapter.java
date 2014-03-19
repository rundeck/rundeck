package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.PathUtil;

import java.util.HashMap;

/**
 * Adapter for a {@link com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin} to use as a {@link StorageConverter}
 */
public class StorageConverterPluginAdapter implements StorageConverter {
    StorageConverterPlugin plugin;

    public StorageConverterPluginAdapter(StorageConverterPlugin plugin) {
        this.plugin = plugin;
    }

    static enum Operation {READ, UPDATE, CREATE}

    /**
     * perform appropriate plugin filter method based on the operation enacted
     *
     * @param path         path
     * @param resourceMeta resource
     * @param op           operation
     *
     * @return new resource
     */
    private ResourceMeta filter(Path path, ResourceMeta resourceMeta, Operation op) {
        ResourceMetaBuilder resourceMetaBuilder = ResourceUtil.create(new HashMap<String,
                String>(resourceMeta.getMeta()));
        HasInputStream wrappedStream = PathUtil.wrapStream(resourceMeta);
        final HasInputStream result;
        switch (op) {
            case READ:
                result = plugin.readResource(path, resourceMetaBuilder, wrappedStream);
                break;
            case UPDATE:
                result = plugin.updateResource(path, resourceMetaBuilder, wrappedStream);
                break;
            case CREATE:
                result = plugin.createResource(path, resourceMetaBuilder, wrappedStream);
                break;
            default:
                throw new IllegalStateException();
        }
        //construct the new data
        return ResourceUtil.withStream(null == result ? wrappedStream : result, resourceMetaBuilder.getResourceMeta());
    }

    @Override
    public ResourceMeta convertReadData(Path path, ResourceMeta resourceMeta) {
        return filter(path, resourceMeta, Operation.READ);
    }

    @Override
    public ResourceMeta convertCreateData(Path path, ResourceMeta resourceMeta) {
        return filter(path, resourceMeta, Operation.CREATE);
    }

    @Override
    public ResourceMeta convertUpdateData(Path path, ResourceMeta resourceMeta) {
        return filter(path, resourceMeta, Operation.UPDATE);
    }
}

package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.apache.log4j.Logger;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.api.StorageException;

import java.util.HashMap;

/**
 * Adapter for a {@link com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin} to use as a {@link
 * StorageConverter}
 */
public class StorageConverterPluginAdapter implements StorageConverter {
    static final Logger logger = Logger.getLogger(StorageConverterPluginAdapter.class);
    StorageConverterPlugin plugin;
    String providerName;

    public StorageConverterPluginAdapter(String providerName,StorageConverterPlugin plugin) {
        this.providerName=providerName;
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
        ResourceMetaBuilder resourceMetaBuilder = StorageUtil.create(new HashMap<String,
                String>(resourceMeta.getMeta()));
        final HasInputStream result;
        switch (op) {
            case READ:
            case UPDATE:
            case CREATE:
                try {
                    if (op == Operation.CREATE) {
                        result = plugin.createResource(path, resourceMetaBuilder, resourceMeta);
                    } else if (op == Operation.READ) {
                        result = plugin.readResource(path, resourceMetaBuilder, resourceMeta);
                    } else {
                        result = plugin.updateResource(path, resourceMetaBuilder, resourceMeta);
                    }
                } catch (Throwable e) {
                    throw new StorageException("Converter Plugin " + providerName + " threw exception during " + op +
                            ": " + e.getMessage(), e, StorageException.Event.valueOf(op.toString()), path);
                }
                break;
            default:
                throw new IllegalStateException();
        }
        logger.debug(
                "Plugin(" + providerName + "):" + op + ":" + path +
                ";" + (null == result ? "_" : "+") + ":" + resourceMetaBuilder.getResourceMeta()
        );
        //construct the new data
        return StorageUtil.withStream(null == result ? resourceMeta : result, resourceMetaBuilder.getResourceMeta());
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

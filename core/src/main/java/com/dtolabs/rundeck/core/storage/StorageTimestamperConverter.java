package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

import java.util.Date;

/**
 * StorageTimestamperConverter sets modification and creation timestamp metadata for updated/created resources.
 *
 * @author greg
 * @since 2014-03-16
 */
public class StorageTimestamperConverter implements StorageConverterPlugin {
    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        resourceMetaBuilder.setCreationTime(new Date());
        resourceMetaBuilder.setModificationTime(new Date());
        return null;
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream) {
        resourceMetaBuilder.setModificationTime(new Date());
        return null;
    }
}

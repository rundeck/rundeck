package com.dtolabs.rundeck.plugins.storage;

import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

/**
 * Plugin to convert storage resource data
 */
public interface StorageConverterPlugin {

    /**
     * Convert a resource during read operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasInputStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream);

    /**
     * Convert a resource during create operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasInputStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream);

    /**
     * Convert a resource during update operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasInputStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream);
}

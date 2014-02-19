package com.dtolabs.rundeck.plugins.resourcetree;

import com.dtolabs.rundeck.core.resourcetree.HasResourceStream;
import com.dtolabs.rundeck.core.resourcetree.ResourceMetaBuilder;
import us.vario.greg.lct.model.Path;

import java.io.InputStream;

/**
 * Plugin to convert resource data
 */
public interface ResourceConverterPlugin {

    /**
     * Convert a resource during read operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasResourceStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasResourceStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasResourceStream hasResourceStream);

    /**
     * Convert a resource during create operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasResourceStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasResourceStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasResourceStream hasResourceStream);

    /**
     * Convert a resource during update operation and return a new data stream, or null to pass the data without
     * changing.
     *
     * @param path                input path
     * @param resourceMetaBuilder builder to modify metadata
     * @param hasResourceStream   accessor for underlying data stream
     *
     * @return new data stream, or null
     */
    HasResourceStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
            HasResourceStream hasResourceStream);
}

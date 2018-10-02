package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

/**
 * KeyStorageLayer applies content-type requirements
 *
 * @author greg
 * @since 2014-03-19
 */
public class KeyStorageLayer implements StorageConverterPlugin {
    public static final String PRIVATE_KEY_MIME_TYPE = "application/octet-stream";
    public static final String PUBLIC_KEY_MIME_TYPE = "application/pgp-keys";
    public static final String PASSWORD_MIME_TYPE = "application/x-rundeck-data-password";
    public static final String RUNDECK_KEY_TYPE = "Rundeck-key-type";
    public static final String RUNDECK_DATA_TYPE = "Rundeck-data-type";
    public static final String RUNDECK_CONTENT_MASK = "Rundeck-content-mask";
    public static final String CONTENT_MASK_TYPE_CONTENT = "content";
    public static final String KEY_TYPE_PRIVATE = "private";
    public static final String KEY_TYPE_PUBLIC = "public";
    public static final String KEY_TYPE_PASSWORD = "password";

    @Override
    public HasInputStream readResource(
            Path path,
            ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    ) {
        validate(resourceMetaBuilder, path);
        return null;
    }

    @Override
    public HasInputStream createResource(
            Path path,
            ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    ) {
        validate(resourceMetaBuilder, path);
        return null;
    }

    protected void validate(ResourceMetaBuilder resourceMetaBuilder, Path path) {
        String type = resourceMetaBuilder.getContentType();

        if (type.equals(PRIVATE_KEY_MIME_TYPE)) {
            resourceMetaBuilder.setMeta(RUNDECK_CONTENT_MASK, CONTENT_MASK_TYPE_CONTENT);
            resourceMetaBuilder.setMeta(RUNDECK_KEY_TYPE, KEY_TYPE_PRIVATE);
        }
        else if (type.equals(PUBLIC_KEY_MIME_TYPE)) {
            resourceMetaBuilder.setMeta(RUNDECK_KEY_TYPE, KEY_TYPE_PUBLIC);
        }
        else if (type.equals(PASSWORD_MIME_TYPE)) {
            resourceMetaBuilder.setMeta(RUNDECK_CONTENT_MASK, CONTENT_MASK_TYPE_CONTENT);
            resourceMetaBuilder.setMeta(RUNDECK_DATA_TYPE, KEY_TYPE_PASSWORD);
        }

    }

    @Override
    public HasInputStream updateResource(
            Path path,
            ResourceMetaBuilder resourceMetaBuilder,
            HasInputStream hasInputStream
    ) {
        validate(resourceMetaBuilder, path);
        return null;
    }
}

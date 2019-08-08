package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import org.apache.commons.lang.StringUtils;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;

public class FileStorageLayer implements StorageConverterPlugin {
    public static final String FILE_MIME_TYPE = "application/octet-stream";

    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validateMimeType(resourceMetaBuilder);
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validateMimeType(resourceMetaBuilder);
        return null;
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validateMimeType(resourceMetaBuilder);
        return null;
    }

    protected void validateMimeType(ResourceMetaBuilder resourceMetaBuilder){
        String type = resourceMetaBuilder.getContentType();
        if(StringUtils.isEmpty(type)){
            resourceMetaBuilder.setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE, FILE_MIME_TYPE);
        }
    }
}

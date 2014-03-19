package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.StorageException

/**
 * SSHKeyStorageLayer applies content-type requirements
 * @author greg
 * @since 2014-03-19
 */
class SSHKeyStorageLayer implements StorageConverterPlugin {
    public static final String PRIVATE_KEY_MIME_TYPE = "application/octet-stream"
    public static final String PUBLIC_KEY_MIME_TYPE = "application/pgp-keys"
    public static final String RUNDECK_SSH_KEY_TYPE = "Rundeck-ssh-key-type"

    @Override
    HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        return null
    }

    @Override
    HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validate(resourceMetaBuilder, path)
        return null;
    }

    protected void validate(ResourceMetaBuilder resourceMetaBuilder, Path path) {
        def type = resourceMetaBuilder.contentType

        if (null == type || (!type.equals(PRIVATE_KEY_MIME_TYPE) && !type.equals(PUBLIC_KEY_MIME_TYPE))) {
            throw StorageException.createException(path, "Content type not allowed: " + type)
        }
        if (type.equals(PRIVATE_KEY_MIME_TYPE)) {
            resourceMetaBuilder.setMeta('Rundeck-content-mask', 'content')
            resourceMetaBuilder.setMeta(RUNDECK_SSH_KEY_TYPE, 'private')
        }else{
            resourceMetaBuilder.setMeta(RUNDECK_SSH_KEY_TYPE, 'public')
        }
    }

    @Override
    HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder, HasInputStream hasInputStream) {
        validate(resourceMetaBuilder, path)
        return null
    }
}

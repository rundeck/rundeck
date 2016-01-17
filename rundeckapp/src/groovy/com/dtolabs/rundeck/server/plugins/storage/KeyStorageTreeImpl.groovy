package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.ResourceMeta
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.Tree

/**
 * Implements {@link KeyStorageTree} and throws {@link WrongContentType} if
 * requested resource does not match the given content type.
 */
class KeyStorageTreeImpl extends TypedStorageTreeImpl implements KeyStorageTree {
    KeyStorageTreeImpl(final Tree<ResourceMeta> delegate) {
        super(delegate)
    }

    @Override
    Resource<ResourceMeta> getPassword(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE)
    }


    @Override
    byte[] readPassword(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PASSWORD_MIME_TYPE)
    }


    @Override
    byte[] readPassword(final String path) {
        return readPassword(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPrivateKey(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPrivateKey(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PRIVATE_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPrivateKey(final String path) {
        readPrivateKey(PathUtil.asPath(path))
    }

    @Override
    Resource<ResourceMeta> getPublicKey(final Path path) {
        getResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPublicKey(final Path path) {
        readResourceWithType(path, KeyStorageLayer.PUBLIC_KEY_MIME_TYPE)
    }

    @Override
    byte[] readPublicKey(final String path) {
        readPublicKey(PathUtil.asPath(path))
    }
}

package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.storage.StorageTree

/**
 * Utility
 */
class KeyStorageUtil {
    /**
     * Wrap a StorageTree with KeyStorageTree capability
     * @param tree
     * @return
     */
    static KeyStorageTree keyStorageWrapper(StorageTree tree) {
        return new KeyStorageTreeImpl(tree)
    }
}

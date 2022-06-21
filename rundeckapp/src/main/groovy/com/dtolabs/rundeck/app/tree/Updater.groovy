package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import groovy.transform.CompileStatic

@CompileStatic
interface Updater {
    /**
     * Perform updates to the tree
     * @param storageTree tree
     * @param basePath base path of the tree to update
     * @param updaterConfig config
     */
    void updateTree(StorageTree storageTree, String basePath, UpdaterConfig updaterConfig)
}

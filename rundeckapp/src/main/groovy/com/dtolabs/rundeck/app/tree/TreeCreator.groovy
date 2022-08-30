package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree

interface TreeCreator {
    StorageTree create(Boolean startup)
    Map<String, String> getStorageConfigMap()
}
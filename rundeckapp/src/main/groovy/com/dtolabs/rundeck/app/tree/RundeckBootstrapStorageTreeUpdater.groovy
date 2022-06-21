package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

/**
 * Wraps calls to the update method with a gorm transaction, and performs the update on bootstrap event
 */
@CompileStatic
class RundeckBootstrapStorageTreeUpdater {

    StorageTree storageTree
    UpdaterConfig updaterConfig

    Boolean enabled
    String basePath
    Updater updater = new TreeUpdater()


    @Subscriber("rundeck.bootstrap")
    void bootstrapEvent() {
        performTreeUpdate()
    }

    @Transactional
    void performTreeUpdate() {
        if (!enabled) {
            return
        }
        updater.updateTree(storageTree, basePath, updaterConfig)
    }
}

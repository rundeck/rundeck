package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.StorageTree
import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Wraps calls to the update method with a gorm transaction, and performs the update on bootstrap event
 */
@CompileStatic
@Slf4j
class RundeckBootstrapStorageTreeUpdater {

    StorageTree storageTree
    UpdaterConfig updaterConfig

    Boolean enabled
    String basePath
    Updater updater = new TreeUpdater()


    @Subscriber("rundeck.bootstrap")
    void bootstrapEvent() {
        log.info("rundeck.bootstrap:RundeckBootstrapStorageTreeUpdater:bootstrapEvent:init bootstrap")
        try {
            performTreeUpdate()
        } catch (Throwable t) {
            log.error("Error during storage tree update: ${t.getMessage()}", t)
        }
        log.info("rundeck.bootstrap:RundeckBootstrapStorageTreeUpdater:bootstrapEvent:end bootstrap")
    }

    @Transactional
    void performTreeUpdate() {
        if (!enabled) {
            return
        }
        updater.updateTree(storageTree, basePath, updaterConfig)
    }
}

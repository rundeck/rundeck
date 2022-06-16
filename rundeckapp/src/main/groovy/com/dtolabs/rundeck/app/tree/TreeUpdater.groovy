package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.storage.api.Resource
/**
 * Iterates over given storage tree, and updates resources according to the config
 */
@CompileStatic
@Slf4j
class TreeUpdater implements Updater {

    void updateTree(StorageTree storageTree, String basePath, UpdaterConfig updaterConfig) {
        if (!updaterConfig.shouldPerform()) {
            return
        }
        //detect if configuration contains jasypt-encryption
        log.info("Storage Tree Updater: begin: ${updaterConfig.name}")
        int count = 0
        List<String> paths = [basePath]
        while (paths.size() > 0) {
            String dirPath = paths.remove(0)

            if (!storageTree.hasDirectory(dirPath)) {
                log.warn("Skipping a path ${dirPath} - it was not a directory")
                continue
            }

            Set<Resource<ResourceMeta>> dirList = storageTree.listDirectory(dirPath)
            paths.addAll(
                dirList.findAll {
                    it.directory
                }.collect {
                    it.path.toString()
                }
            )

            dirList.findAll{
                !it.directory
            }.each { Resource<ResourceMeta> resource->
                def contents = updaterConfig.getUpdatedContents(resource)
                if (!contents) {
                    log.debug("Not updating path: ${resource.path}")
                    return
                }

                log.info("Updating path: ${resource.path}")
                count++
                storageTree.updateResource(
                    resource.path,
                    contents
                )
            }
        }
        log.info("Storage Tree Updater: completed with ${count} updates: ${updaterConfig.name}")
    }

}

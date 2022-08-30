package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import groovy.transform.CompileStatic
import org.rundeck.storage.api.Resource

/**
 * Configuration for Updater
 */
@CompileStatic
interface UpdaterConfig {

    /**
     *
     * @return updater name
     */
    String getName()

    /**
     *
     * @return true if update should occur
     */
    boolean shouldPerform()

    /**
     *
     * @param resource
     * @return updated contents, or null to skip update
     */
    ResourceMeta getUpdatedContents(Resource<ResourceMeta> resource)
}

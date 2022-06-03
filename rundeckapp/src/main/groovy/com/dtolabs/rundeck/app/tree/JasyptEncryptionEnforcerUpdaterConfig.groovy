package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import groovy.transform.CompileStatic
import org.rundeck.storage.api.Resource

/**
 * Defines update logic for re-storing unencrypted values when jasypt-encryption is enabled, to enforce encrypted
 * content
 */
@CompileStatic
class JasyptEncryptionEnforcerUpdaterConfig implements UpdaterConfig {
    TreeCreator treeCreator


    @Override
    boolean shouldPerform() {
        def configuration = treeCreator.storageConfigMap
        return configuration.entrySet().find {
            it.key =~ /^converter\.\d+\.type/ && it.value == 'jasypt-encryption'
        }
    }

    @Override
    ResourceMeta getUpdatedContents(final Resource<ResourceMeta> resource) {
        def contents = resource.contents

        if (contents.meta.get('jasypt-encryption:encrypted') == 'true') {
            //don't modify existing encrypted data
            return null
        }

        //return original content so it is simply restored without change
        return contents
    }

    final String name = "Jasypt Encryption Enforcement - rewrites unencrypted values"

    @Override
    String toString() {
        name
    }
}

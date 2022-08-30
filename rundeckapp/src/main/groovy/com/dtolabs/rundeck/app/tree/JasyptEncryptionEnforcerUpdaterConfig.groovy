package com.dtolabs.rundeck.app.tree

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.storage.api.Resource

/**
 * Defines update logic for re-storing unencrypted values when jasypt-encryption is enabled, to enforce encrypted
 * content
 */
@CompileStatic
@Slf4j
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
            // There are edge cases that the JSON meta information indicate the content is encrypted,
            // but it actually plain text saved.
            // To tackle this issue, only check the meta `jasypt-encryption:encrypted` is not enough,
            // we also need to verify if the content is really encrypted.
            // We will use the length of the content to compare with the `Rundeck-content-size` meta value,
            // if those two values are exact the same then we can confirm the saved value is not encrypted.
            //
            // Due to there is no guaranteed way to check if the content is encrypted or not, so we use this
            // opt-in style implementation to limit the logic only applied to the specific error case.

            try {
                if(contents.getInputStream().getBytes().size() ==
                        Integer.parseInt(contents.meta.get(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH))) {
                    return contents
                }
            } catch(Exception ex) {
                // Suppress the error, we don't want to block the application startup.
                log.info(String.format("Encounter error [%s] when checking if storage key at path [%s] is properly encrypted. Suppressed.",
                        ex.getMessage(), resource.path));
            }
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

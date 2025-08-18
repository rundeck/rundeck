package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.FeaturesDefinition
import com.dtolabs.rundeck.core.execution.service.NodeExecutorServiceProfile
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

/**
 * Switches between two profiles based on the presence of a feature
 */
@CompileStatic
class FeatureToggleNodeExecutorProfile implements NodeExecutorServiceProfile {
    /**
     * Base profile
     */
    NodeExecutorServiceProfile baseProfile
    /**
     * Profile to use when feature is present
     */
    NodeExecutorServiceProfile toggleProfile
    /**
     * Feature to check for
     */
    FeaturesDefinition feature

    @Autowired
    FeatureService featureService

    @Delegate
    NodeExecutorServiceProfile getDelegate() {
        if (featureService.featurePresent(feature)) {
            return toggleProfile
        } else {
            return baseProfile
        }
    }
}

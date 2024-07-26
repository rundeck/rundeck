package org.rundeck.app.spi.features

import com.dtolabs.rundeck.core.config.FeatureInfoService
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.FeaturesDefinition
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy

/**
 * Provide read access to feature flags
 */
@CompileStatic
class FeatureInfo implements FeatureInfoService {
    @Autowired
    @Lazy
    FeatureService featureService

    @Override
    boolean featurePresent(final FeaturesDefinition feature) {
        return featureService.featurePresent(feature)
    }

    @Override
    boolean featurePresent(final String name) {
        return featureService.featurePresent(name)
    }

    @Override
    boolean featurePresent(final FeaturesDefinition feature, final boolean defaultEnabled) {
        return featureService.featurePresent(feature, defaultEnabled)
    }

    @Override
    boolean featurePresent(final String name, final boolean defaultEnabled) {
        return featureService.featurePresent(name, defaultEnabled)
    }
}

/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services.feature

import com.dtolabs.rundeck.core.config.Features

/**
 * Manage feature configuration in the 'rundeck.feature.X' namespace, a
 * property 'feature.enableAll' enables all features.
 */
class FeatureService {
    static transactional = false
    def configurationService

    /**
     * Return true if grails configuration allows given feature
     * @param feature
     * @return
     */
    def boolean featurePresent(Features feature) {
        featurePresent(feature.propertyName)
    }
    /**
     * Return true if grails configuration allows given feature
     * @param name
     * @return
     */
    def boolean featurePresent(def name) {
        featurePresent(name, false)
    }

    /**
     * Return true if grails configuration allows given feature
     * @param feature
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    def boolean featurePresent(Features feature, boolean defaultEnabled) {
        featurePresent(feature.propertyName, defaultEnabled)
    }

    /**
     * Return true if grails configuration allows given feature
     * @param name
     * @param defaultEnabled default enabled value for the feature, if unset
     * @return true if enabled
     */
    def boolean featurePresent(def name, boolean defaultEnabled) {
        def splat = configurationService.getBoolean('feature.enableAll', false)
        return splat || configurationService.getBoolean("feature.${name}.enabled", defaultEnabled)
    }
    /**
     * Set an incubator feature toggle on or off
     * @param feature
     * @param enable
     */
    def void toggleFeature(Features feature, boolean enable) {
        configurationService.setBoolean("feature.${feature.propertyName}.enabled", enable)
    }
    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    def void toggleFeature(def name, boolean enable) {
        configurationService.setBoolean("feature.${name}.enabled", enable)
    }
    /**
     * Get feature config
     * @param name
     * @param enable
     */
    def getFeatureConfig(Features feature) {
        configurationService.getConfig("feature.${feature.propertyName}.config")
    }
    /**
     * Get feature config
     * @param name
     * @param enable
     */
    def getFeatureConfig(def name) {
        configurationService.getConfig("feature.${name}.config")
    }
}

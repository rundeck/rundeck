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

/**
 * Manage feature configuration in the 'rundeck.feature.X' namespace, a
 * feature '*' enables all features.
 */
class FeatureService {
    static transactional = false
    def configurationService
    /**
     * Return true if grails configuration allows given feature, or '*' features
     * @param name
     * @return
     */
    def boolean featurePresent(def name) {
        def splat = configurationService.getBoolean('feature.*.enabled', false)
        return splat || configurationService.getBoolean("feature.${name}.enabled", false)
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
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    def getFeatureConfig(def name) {
        configurationService.getConfig("feature.${name}.config")
    }
}

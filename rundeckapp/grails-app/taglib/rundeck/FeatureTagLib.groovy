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

package rundeck

import rundeck.services.feature.FeatureService

class FeatureTagLib {
    def static namespace = "feature"
    def FeatureService featureService

    static returnObjectForTags = ['isEnabled', 'isDisabled']
    /**
     * Return true if the feature 'name' is enabled
     * @attr name REQUIRED name of feature
     */
    def isEnabled = { attrs, body ->
        if (!attrs.name) {
            throw new Exception("attribute required: 'name'")
        }
        return featureService.featurePresent(attrs.name)
    }
    /**
     * Render body if the feature is enabled
     * @attr name REQUIRED name of feature
     */
    def enabled = { attrs, body ->
        if (isEnabled(attrs, body)) {
            out << body()
        }
    }
    /**
     * Return true if the feature 'name' is disabled
     * @attr name REQUIRED name of feature
     */
    def isDisabled = { attrs, body ->
        return !isEnabled(attrs, body)
    }
    /**
     * Render body if the feature is disabled
     * @attr name REQUIRED name of feature
     */
    def disabled = { attrs, body ->
        if (isDisabled(attrs, body)) {
            out << body()
        }
    }
}

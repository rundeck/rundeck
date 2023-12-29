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

import org.rundeck.security.RundeckPluginBlocklist

class BuiltInBlockListTagLib {
    def static namespace = "blocklist"
    def RundeckPluginBlocklist rundeckPluginBlocklist

    static returnObjectForTags = ['isProviderPresent']
    /**
     * Return true if the feature 'name' is enabled
     * @attr name REQUIRED name of feature
     */
    def isProviderPresent = { attrs, body ->
        if (!attrs.name) {
            throw new Exception("attribute required: 'name'")
        }
        if (!attrs.service) {
            throw new Exception("attribute required: 'service'")
        }
        return rundeckPluginBlocklist.isPluginProviderPresent(attrs.service, attrs.name)
    }
    /**
     * Render body if the feature is enabled
     * @attr name REQUIRED name of feature
     */
    def pluginEnabled = { attrs, body ->
        if (!isProviderPresent(attrs, body)) {
            out << body()
        }
    }
}

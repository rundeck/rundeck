/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.services


import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import groovy.transform.CompileStatic

@CompileStatic
class TypedNodeEnhancerPlugin implements NodeEnhancerPlugin {
    @Delegate NodeEnhancerPlugin other
    String type

    TypedNodeEnhancerPlugin(final NodeEnhancerPlugin other, final String type) {
        this.other = other
        this.type = type
    }
}

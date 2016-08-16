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

import com.dtolabs.rundeck.core.common.Framework
import rundeck.services.FrameworkService

class PluginTagLib {
    def static namespace = "stepplugin"
    def FrameworkService frameworkService

    def display={attrs,body->
        def step=attrs.step
        def description = frameworkService.getPluginDescriptionForItem(step)
        if(description){
            out << render(
                    template: "/framework/renderPluginConfig",
                    model: [
                            type: step.type,
                            values: step?.configuration,
                            description: description
                    ] + attrs.subMap(['showPluginIcon','showNodeIcon','prefix', 'includeFormFields'])
            )
            return
        }
        out << "Plugin " + (step.nodeStep ? "Node" : "") + " Step (${step.type})"
    }
}

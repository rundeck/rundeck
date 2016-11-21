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
import org.springframework.web.servlet.support.RequestContextUtils
import rundeck.services.FrameworkService
import rundeck.services.UiPluginService

class PluginTagLib {
    def static namespace = "stepplugin"
    def FrameworkService frameworkService
    def UiPluginService uiPluginService
    static returnObjectForTags = ['messageText']

    def display={attrs,body->
        def step=attrs.step
        def description = frameworkService.getPluginDescriptionForItem(step)
        if(description){
            out << render(
                    template: "/framework/renderPluginConfig",
                    model: [
                            type: step.type,
                            serviceName:step.nodeStep?'WorkflowNodeStep':'WorkflowStep',
                            values: step?.configuration,
                            description: description
                    ] + attrs.subMap(['showPluginIcon','showNodeIcon','prefix', 'includeFormFields'])
            )
            return
        }
        out << "Plugin " + (step.nodeStep ? "Node" : "") + " Step (${step.type})"
    }

    def pluginIcon = { attrs, body ->
        def service = attrs.remove('service')
        def name = attrs.remove('name')

        if (!service || !name) {
            out << body()
            return
        }
        def profile = uiPluginService.getProfileFor(service, name)
        if (profile.icon) {
            attrs.src = createLink(
                    controller: 'plugin',
                    action: 'pluginIcon',
                    params: [service: service, name: name]
            )
            out << '<img '
            attrs.each { k, v ->
                if (v) {
                    out << " ${k}=\"${enc(attr: v)}\""
                }
            }
            out << "/>"
        } else {
            out << body()
        }
    }
    /**
     * Write plugin i18n message or default to html with encoding
     *
     * @attr service service name
     * @attr name provider name
     * @attr code message code
     * @attr default default value if not found
     */
    def message = { attrs, body ->
        out << g.enc(html: messageText(attrs, body))
    }
    /**
     * Return plugin i18n message or default without encoding it
     *
     *
     * @attr service service name
     * @attr name provider name
     * @attr code message code
     * @attr default default value if not found
     */
    def messageText = { attrs, body ->
        def service = attrs.service
        def plugin = attrs.name
        def code = attrs.code
        def defaultmsg = attrs.default
        def messages = [:]
        if (service && plugin) {
            messages = uiPluginService.getMessagesFor(service, plugin, RequestContextUtils.getLocale(request))
        }
        def foundcode = [
                service + '.' + plugin + '.' + code,
                plugin + '.' + code,
                service + '.' + code,
                code,
        ].find { messages[it] }
        return (foundcode != null ? messages[foundcode] : defaultmsg)
    }
}

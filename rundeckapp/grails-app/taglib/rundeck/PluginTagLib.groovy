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
import groovy.json.JsonSlurper

class PluginTagLib {
    def static namespace = "stepplugin"
    def FrameworkService frameworkService
    def UiPluginService uiPluginService
    static returnObjectForTags = ['messageText','pluginIconSrc','pluginProviderMeta','customFields']

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

    /**
     * @attr prop REQUIRED Property object
     */
    def propertyIcon = { attrs, body ->
        def prop = attrs.get('prop')
        def options=prop.renderingOptions?:[:]
        if (options.get('glyphicon')) {
            out << "<i class=\"glyphicon glyphicon-${enc(attr: options.get('glyphicon'))}\"></i>"
        } else if (options.get('faicon')) {
            out << "<i class=\"fas fa-${enc(attr: options.get('faicon'))}\"></i>"
        } else if (options.get('fabicon')) {
            out << "<i class=\"fab fa-${enc(attr: options.get('fabicon'))}\"></i>"
        } else if(options.get('icon:plugin:serviceName') && options.get('icon:plugin:provider')) {
            out << pluginIcon(service: options.get('icon:plugin:serviceName'), name: options.get('icon:plugin:provider'), body)
        }
    }
    def pluginIcon = { attrs, body ->
        def service = attrs.get('service')
        def name = attrs.get('name')
        if(!service || !name){
            throw new Exception("service and name attributes required")
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
        } else if (profile.providerMetadata?.glyphicon) {
            out << g.icon(name: profile.providerMetadata?.glyphicon)
        } else if (profile.providerMetadata?.faicon) {
            out << '<i class="'
            out << enc(attr: 'fas fa-' + profile.providerMetadata?.faicon)
            out << '"></i>'
        } else if (profile.providerMetadata?.fabicon) {
            out << '<i class="'
            out << enc(attr: 'fab fa-' + profile.providerMetadata?.fabicon)
            out << '"></i>'
        } else {
            out << body()
        }
    }
    def pluginIconSrc = { attrs, body ->
        def service = attrs.remove('service')
        def name = attrs.remove('name')

        if (!service || !name) {
            return null
        }
        def profile = uiPluginService.getProfileFor(service, name)
        if (profile.icon) {
            return createLink(
                    controller: 'plugin',
                    action: 'pluginIcon',
                    params: [service: service, name: name]
            )
        }
        return null
    }
    def pluginProviderMeta = { attrs, body ->
        def service = attrs.remove('service')
        def name = attrs.remove('name')

        if (!service || !name) {
            return null
        }
        def profile = uiPluginService.getProfileFor(service, name)
        if (profile.providerMetadata) {
            return profile.providerMetadata
        }
        return null
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
        def messagesType = attrs.messagesType
        def messages = [:]
        if (!messagesType || messagesType in ['plugin']) {
            if (service && plugin) {
                return uiPluginService.getPluginMessage(service, plugin, code, defaultmsg, RequestContextUtils.getLocale(request))
            }
            return defaultmsg
        } else if (messagesType) {
            def testCodes = [messagesType + '.' + code, code]
            for (def c : testCodes) {
                def msg = g.message(code: c, default: c)
                if(msg!=c){
                    return msg
                }
            }
            return defaultmsg
        }
    }

    /**
     * Return plugin i18n message or default without encoding it
     *
     *
     * @attr json provider name
     */
    def customFields = { attrs, body ->
        if(attrs.json){
            def json = attrs.json
            def jsonSlurper = new JsonSlurper()
            try{
                def fields = jsonSlurper.parseText(json)
                out << '<div class="customattributes"></div>'
                fields.each{field->
                    out << '<span class="configpair">'
                    out << '<span title="">'+field.label.encodeAsSanitizedHTML()+': </span>'
                    out << '<span class="text-success">'+field.value.encodeAsSanitizedHTML()+'</span>'
                    out << '</span>'
                }
                out << ''
            }catch(Exception e){
                log.warn(e.message)
            }
        }
    }
}

%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
<%--
   _pluginConfigPropertyValue.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 7/28/11 12:03 PM
--%>
<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" contentType="text/html;charset=UTF-8" %>
<g:set var="propkey" value="${g.rkey()}"/>

<g:embedJSON id="${propkey}_json" data="${[
    service              : service,
    provider             : provider,
    name                 : prop.name,
    value                : values?.get(prop.name),
    origfieldname        : (origfieldnamePrefix ?: '') + prop.name,
    fieldname            : (fieldnamePrefix ?: '') + prop.name,
    type                 : prop.type.toString(),
    project              : project ?: params.project ?: request.project,
    renderingOptions     : prop.renderingOptions,
    hasEmbeddedType      : prop.embeddedType != null,
    hasEmbeddedPluginType: prop.embeddedPluginType != null,
    previewMode          : true,
    embeddedServiceName  : prop.embeddedPluginType && pluginServicesByClass? pluginServicesByClass[prop.embeddedPluginType] : null,
    idkey                : idkey

]}"/>
<g:javascript>"use strict";

    jQuery(function () {
        if(typeof(window.PluginProperties)!=='object'){
            window.PluginProperties={};
        }
        const json=loadJsonData('${propkey}_json');
        const prop = new PluginProperty(json);
        PluginProperties['${propkey}']=prop;

        initKoBind('#${propkey}',{pluginProperty:prop});
    })
</g:javascript>
<span id="${propkey}">
<g:set var="propDescription" value="${stepplugin.messageText(
        service: service,
        name: provider,
        code: (messagePrefix ?: '') + 'property.' + prop.name + '.description',
        default: prop.description
)}"/>
<g:set var="propdesc" value="${g.textFirstLine(text: propDescription)}"/>
    <g:if test="${prop.type.toString() == 'Boolean'}">
        <g:if test="${values[prop.name] == 'true'}">
            <span class="configpair">
                <span title="${enc(attr: propdesc)}"><stepplugin:message
                        service="${service}"
                        name="${provider}"
                        code="${messagePrefix}property.${prop.name}.title"
                        default="${prop.title ?: prop.name}"/>:</span>
                <g:set var="textclass" value="text-success"/>
                <g:if test="${prop.renderingOptions['booleanTrueDisplayValueClass']}">
                    <g:set var="textclass" value="${prop.renderingOptions['booleanTrueDisplayValueClass']}"/>
                </g:if>
                <span class="${textclass}"><g:message code="yes"/></span>
            </span>
        </g:if>
</g:if>
<g:elseif test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.PASSWORD, 'PASSWORD']}">
    <g:if test="${values[prop.name]}">
        <span class="configpair">
            <span title="${enc(attr: propdesc)}"><stepplugin:message
                    service="${service}"
                    name="${provider}"
                    code="${messagePrefix}property.${prop.name}.title"
                    default="${prop.title ?: prop.name}"/>:</span>
            <span class="text-success">&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;</span>
        </span>
    </g:if>
</g:elseif>
<g:elseif test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in [StringRenderingConstants.DisplayType.CODE, 'CODE']}">
    <g:set var="rakey" value="${g.rkey()}"/>
    <g:set var="script" value="${values[prop.name] ?: ''}"/>
    <g:set var="split" value="${script.split('(\r?\n)') as List}"/>

    <span class="configpair">
        <span title="${enc(attr: propdesc)}"><stepplugin:message
                service="${service}"
                name="${provider}"
                code="${messagePrefix}property.${prop.name}.title"
                default="${prop.title ?: prop.name}"/>:</span>
        <g:collapser key="${rakey}"><g:enc>${label ? label : ''}</g:enc>${split.size()} lines</g:collapser>
        <div class="collapse collapse-expandable" id="${enc(attr:rakey)}">
            <div class="scriptContent apply_ace"><g:enc>${script}</g:enc></div>
        </div>
    </span>
</g:elseif>
    <g:elseif test="${prop.renderingOptions?.(StringRenderingConstants.DISPLAY_TYPE_KEY) in ['RUNDECK_JOB']}">
        <span class="configpair" data-ko-controller="pluginProperty">
            <span title="${enc(attr: propdesc)}"><stepplugin:message
                    service="${service}"
                    name="${provider}"
                    code="${messagePrefix}property.${prop.name}.title"
                    default="${prop.title ?: prop.name}"/>:</span>

            <span data-bind="visible: !toggle.value()">
                <span data-bind="if: value">
                    <job-link params="id: value, project:project"></job-link>
                </span>
                <span data-bind="if: !value()" class="text-info">
                    No Job Set
                </span>
            </span>



            <a href="#" class="text-success" data-bind="attr: {title: value}, click: toggle.toggle">
                <g:icon name="info-sign"/></a>

            <span data-bind="visible: toggle.value" class="text-muted">
                <g:enc>${values[prop.name]}</g:enc>
            </span>

        </span>

    </g:elseif>
    <g:elseif test="${prop.type.toString() == 'Embedded'}">
        <g:if test="${prop.embeddedType != null || prop.embeddedPluginType != null}">

            <div data-ko-controller="pluginProperty" class="embedded-plugin" style="margin-left: 10px">
                <span title="${enc(attr: propdesc)}"><stepplugin:message
                    service="${service}"
                    name="${provider}"
                    code="${messagePrefix}property.${prop.name}.title"
                    default="${prop.title ?: prop.name}"/>:</span>

                <plugin-editor
                    params="editor: editor, typeField: fieldname()+'._type', embeddedTypeField: fieldname()+'.type', propname:name"></plugin-editor>
            </div>

        </g:if>
    </g:elseif>
<g:elseif test="${values[prop.name]}">
    <span class="configpair">
        <span title="${enc(attr: propdesc)}"><stepplugin:message
                service="${service}"
                name="${provider}"
                code="${messagePrefix}property.${prop.name}.title"
                default="${prop.title ?: prop.name}"/>:</span>

        <g:if test="${prop.type.toString() in ['Options', 'Select', 'FreeSelect']}">

            <g:set var="propSelectLabels" value="${prop.selectLabels ?: [:]}"/>
            <g:set var="defval" value="${values && null != values[prop.name] ? values[prop.name] : prop.defaultValue}"/>

            <g:if test="${prop.type.toString() in ['Select', 'FreeSelect']}">
                <span class="text-success">${propSelectLabels[defval] ?: defval}</span>
            </g:if>
            <g:else>
                <g:set var="defvalset" value="${defval && defval instanceof String ? defval.split(', *') :
                                                defval && defval instanceof Collection ? defval : []}"/>
                <span class="text-success">
                    <g:each in="${defvalset}" var="optval">
                        <span class="text-success"><g:icon name="ok-circle"/> ${propSelectLabels[optval] ?: optval}</span>
                    </g:each>
                </span>
            </g:else>

        </g:if>
        <g:elseif test="${prop.type.toString() in ['Map']}">

            <g:set var="defval" value="${values && null != values[prop.name] ? values[prop.name] : prop.defaultValue}"/>

            <g:if
                test="${prop.renderingOptions?.(StringRenderingConstants.SELECTION_ACCESSOR_KEY) in [StringRenderingConstants.SelectionAccessor.RUNDECK_JOB_OPTIONS, 'RUNDECK_JOB_OPTIONS']}">
                <g:set var="defvalset" value="${defval && defval instanceof Map ? defval : [:]}"/>
                <span class="text-success">
                    <g:each in="${defvalset}" var="mapentry">
                        <span class="text-info">-${mapentry.key}</span> <span class="configvalue">${mapentry.value}</span>
                    </g:each>
                </span>
            </g:if>
            <g:else>
                <g:set var="defvalset" value="${defval && defval instanceof Map ? defval : [:]}"/>
                <span class="text-success">
                    <g:each in="${defvalset}" var="mapentry">
                        <span class="text-info">${mapentry.key}</span>:
                        <span class="configvalue">${mapentry.value}</span>
                    </g:each>
                </span>
            </g:else>

        </g:elseif>
        <g:else>
            <span class="text-success"><g:enc>${values[prop.name]}</g:enc></span>
        </g:else>
    </span>
</g:elseif>
</span>
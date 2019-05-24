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

<g:set var="propDescription" value="${stepplugin.messageText(
        service: service,
        name: provider,
        code: (messagePrefix?:'')+'property.' + prop.name + '.description',
        default: prop.description
)}"/>
<g:set var="propdesc" value="${g.textFirstLine(text: propDescription)}"/>
<g:if test="${prop.type.toString()=='Boolean'}">
    <g:if test="${values[prop.name]=='true'}">
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
    <g:set var="script" value="${values[prop.name]?:''}"/>
    <g:set var="split" value="${script.split('(\r?\n)') as List}"/>

    <span class="configpair">
        <details class="more-info details-reset">
            <summary >
                <span title="${enc(attr: propdesc)}"><stepplugin:message
                        service="${service}"
                        name="${provider}"
                        code="${messagePrefix}property.${prop.name}.title"
                        default="${prop.title ?: prop.name}"/>:</span>

                <span class="text-info">${split.size()} lines</span>
                <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
                <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
            </summary>
            <div class="scriptContent apply_ace"><g:enc>${script}</g:enc></div>
        </details>
    </span>
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
                <g:set var="defvalset" value="${defval ? defval.split(', *') : []}"/>
                <span class="text-success">
                    <g:each in="${defvalset}" var="optval">
                        <span class="text-success"><g:icon name="ok-circle"/> ${propSelectLabels[optval] ?: optval}</span>
                    </g:each>
                </span>
            </g:else>

        </g:if>
        <g:else>
            <span class="text-success"><g:enc>${values[prop.name]}</g:enc></span>
        </g:else>
    </span>
</g:elseif>

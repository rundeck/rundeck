%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
  Created by IntelliJ IDEA.
  User: greg
  Date: 2/5/18
  Time: 2:06 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.core.plugins.configuration.Description" %>

<g:set var="pluginName" value="${pluginDescription.name}"/>
<g:set var="prefix" value="${(inputFieldPrefix)}"/>
<g:set var="pluginkey" value="${g.rkey()}"/>

<g:set var="definedConfig" value="${config}"/>
<div data-plugin-name="${pluginName}" data-plugin-service="${service}" class="plugin-config">

    <g:render template="/framework/renderPluginDesc" model="${[
        serviceName    : service,
        description    : pluginDescription,
        showPluginIcon : true,
        showNodeIcon   : showNodeIcon,
        hideTitle      : hideTitle,
        hideDescription: hideDescription,
        fullDescription: fullDescription
    ]}"/>

    <div>

        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
            service                : service,
            provider               : pluginDescription.name,
            properties             : pluginDescription?.properties,
            dynamicProperties      : dynamicProperties,
            dynamicPropertiesLabels: dynamicPropertiesLabels,
            report                 : report,
            prefix                 : prefix,
            values                 : definedConfig,
            fieldnamePrefix        : "${prefix}config.",
            origfieldnamePrefix    : "orig.${prefix}config.",
            allowedScope           : PropertyScope.Instance,
            idkey                  : pluginkey
        ]}"/>

    </div>
</div>

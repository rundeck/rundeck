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
  Date: 3/8/18
  Time: 4:34 PM
--%>


<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.core.plugins.configuration.Description" %>

<g:set var="pluginName" value="${pluginDescription.name}"/>
<g:set var="prefix" value="${(inputFieldPrefix)}"/>
<g:set var="pluginkey" value="${g.rkey()}"/>

<g:set var="definedConfig" value="${config}"/>
<div data-plugin-name="${pluginName}" data-plugin-service="${service}" class="plugin-preview">

    <g:render template="/framework/renderPluginConfig" model="${[
        serviceName      : service,
        type             : pluginDescription.name,
        description      : pluginDescription,
        showPluginIcon   : true,
        values           : definedConfig,
        allowedScope     : PropertyScope.Instance,
        idkey            : pluginkey,
        includeFormFields: true,
        prefix           : inputFieldPrefix
    ]}"/>

</div>

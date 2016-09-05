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

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:set var="orchestrator" value="${scheduledExecution.orchestrator}"/>
<g:set var="desc" value="${orchestratorPlugins?.getDescription(orchestrator.type)}"/>
<g:if test="${desc && desc instanceof Description}">
    <g:expander key="orchestratorplugin${orchestrator.type}">${desc.title.encodeAsHTML()} </g:expander>
    <span class="" id="orchestratorplugin${orchestrator.type}" style="display:none;" title="">
        <g:render template="/framework/renderPluginConfig"
                  model="${[serviceName:'Orchestrator',values: orchestrator.configuration, description: desc, hideTitle: true]}"/>
    </span>
</g:if>
<g:elseif test="${!orchestratorPlugins?.getDescription(orchestrator.type)}">
    <span class="warn note"><g:message code="plugin.not.found.0" args="[orchestrator.type?.encodeAsHTML()]" /></span>
</g:elseif>
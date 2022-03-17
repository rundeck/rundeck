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
<g:set var="desc" value="${orchestratorPlugins?.get(orchestrator.type)}"/>
<g:if test="${desc && desc instanceof Description}">
    <details id="exec_detail__orchestrator" class="details-reset more-info">
        <summary>${desc.title.encodeAsHTML()}
            <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
            <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
        </summary>
        <div id="orchestratorplugin_${orchestrator.type}">
            <g:render template="/framework/renderPluginConfig"
                      model="${[serviceName:'Orchestrator',values: orchestrator.configuration, description: desc, hideTitle: true]}"/>
        </div>
    </details>

</g:if>
<g:elseif test="${!orchestratorPlugins?.get(orchestrator.type)}">
    <span class="warn note"><g:message code="plugin.not.found.0" args="[orchestrator.type?.encodeAsHTML()]" /></span>
</g:elseif>
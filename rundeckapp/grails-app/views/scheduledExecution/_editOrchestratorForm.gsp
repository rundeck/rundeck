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

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:if test="${orchestratorPlugins}">
    <div class="form-group">
        <div class="${labelColSize} control-label text-form-label">
            <g:message code="scheduledExecution.property.orchestrator.label"/>
        </div>
        <g:set var="setOrchestrator" value="${params?.orchestratorId ?: scheduledExecution?.orchestrator?.type}"/>
        <div class="${fieldColSize}">
            <g:select name="orchestratorId" optionKey="name" optionValue="title" from="${orchestratorPlugins}" noSelection="${['':'']}" value="${setOrchestrator}" class="form-control"/>

            <span class="help-block">
                <g:message code="scheduledExecution.property.orchestrator.description"/>
            </span>

            <g:each in="${orchestratorPlugins}" var="pluginDescription">
                <g:set var="pluginName" value="${pluginDescription.name}"/>
                <g:set var="prefix" value="${('orchestratorPlugin.'+ pluginName + '.config.')}"/>
                <g:set var="definedNotif" value="${setOrchestrator == pluginName ? scheduledExecution?.orchestrator : null}"/>
                <g:set var="definedConfig"
                    value="${params.orchestratorPlugin?.get(pluginName)?.config ?: definedNotif?.configuration}"/>
                <span id="orchestratorPlugin${pluginName}" style="${wdgt.styleVisible(if: setOrchestrator == pluginName ? true : false)}"
                      class="orchestratorPlugin">
                    <span class="text-info">
                        <g:render template="/scheduledExecution/description"
                                  model="[description: pluginDescription.description,
                                          textCss: '',
                                          mode: 'collapsed',
                                          moreText:'More Information',
                                          rkey: g.rkey()]"/>
                    </span>
                <div>

                        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                properties:pluginDescription?.properties,
                                report:null,
                                prefix:prefix,
                                values:definedConfig,
                                fieldnamePrefix:prefix,
                                origfieldnamePrefix:'orig.' + prefix,
                                allowedScope:PropertyScope.Instance
                        ]}"/>

                </div>
                </span>
                <wdgt:eventHandler for="orchestratorId" equals="${pluginName}"
                                   target="orchestratorPlugin${pluginName}" visible="true"/>
        </g:each>


        </div>
    </div>
</g:if>
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

<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.core.plugins.configuration.Description" %>
<g:if test="${orchestratorPlugins}">
    <div class="form-group">
        <div class="${labelColSize} control-label text-form-label">
            <g:message code="scheduledExecution.property.orchestrator.label"/>
        </div>
        <g:set var="setOrchestrator" value="${params?.orchestratorId ?: scheduledExecution?.orchestrator?.type}"/>
        <div class="${fieldColSize}">
            <select name="orchestratorId" value="${setOrchestrator}" class="form-control">
                <option name="" ${!setOrchestrator ? 'selected' : ''}></option>
                <g:each in="${orchestratorPlugins}" var="plugin">
                    <option value="${plugin.name}" ${setOrchestrator == plugin.name ? 'selected' :
                            ''}><stepplugin:message
                            service="Orchestrator"
                            name="${plugin.name}"
                            code="plugin.title"
                            default="${plugin.title ?: plugin.name}"/></option>
                </g:each>
            </select>

            <span class="help-block">
                <g:message code="scheduledExecution.property.orchestrator.description"/>
            </span>

            <g:each in="${orchestratorPlugins}" var="pluginDescription">
                <g:set var="pluginName" value="${pluginDescription.name}"/>
                <g:set var="prefix" value="${('orchestratorPlugin.'+ pluginName + '.config.')}"/>
                <g:set var="definedNotif" value="${setOrchestrator == pluginName ? scheduledExecution?.orchestrator : null}"/>
                <g:set var="definedConfig"
                    value="${params.orchestratorPlugin?.get(pluginName)?.config ?: definedNotif?.configuration}"/>
                <span data-orchestrator="${pluginName}"  style="${wdgt.styleVisible(if: setOrchestrator == pluginName ? true : false)}"
                      class="orchestratorPlugin">
                    <span class="text-info">
                        <g:render template="/scheduledExecution/description"
                                  model="[description: stepplugin.messageText(
                                          service: ServiceNameConstants.Orchestrator,
                                          name: pluginName,
                                          code: 'plugin.description',
                                          default: pluginDescription.description
                                  ),
                                          textCss    : '',
                                          mode       : 'collapsed',
                                          moreText   : message(code: 'more.information'),
                                          rkey       : g.rkey()]"/>
                    </span>
                <div>

                        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                service            : ServiceNameConstants.Orchestrator,
                                provider           : pluginDescription.name,
                                properties         : pluginDescription?.properties,
                                report             :null,
                                prefix             :prefix,
                                values             :definedConfig,
                                fieldnamePrefix    :prefix,
                                origfieldnamePrefix:'orig.' + prefix,
                                allowedScope       :PropertyScope.Instance
                        ]}"/>

                </div>
                </span>

        </g:each>
            <g:javascript>jQuery(function () {
                "use strict";
                jQuery('[name="orchestratorId"]').on('change', function (d) {
                    jQuery('.orchestratorPlugin').hide();
                    var val = jQuery(this).val();
                    if (val) {
                        jQuery('.orchestratorPlugin[data-orchestrator="' + val+'"]').show();
                    }

                });
            });
            </g:javascript>


        </div>
    </div>
</g:if>
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

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" %>

<g:set var="dynamicProperties" value="${dynamicProperties}"/>
<g:set var="groupMap" value="${g.groupPluginProperties([properties: g.filterPluginPropertiesByFeature(properties:properties), allowedScope: allowedScope])}"/>
<g:set var="groupSet" value="${groupMap.groupSet?:[:]}"/>
<g:set var="secondary" value="${groupMap.secondary?:[]}"/>
<g:set var="ungrouped" value="${groupMap.ungrouped?:[]}"/>

%{--Render ungrouped--}%
<g:each in="${ungrouped}" var="prop">
    <g:render
            template="/framework/pluginConfigPropertyFormField"
            model="${[prop             : prop,
                      dynamicProperties: dynamicProperties ? dynamicProperties[prop.name] : null,
                      prefix           : prefix,
                      error            : report?.errors ? report.errors[prop.name] : null,
                      values           : values,
                      fieldname        : (fieldnamePrefix ?: '') + prop.name,
                      origfieldname    : (origfieldnamePrefix ?: '') + prop.name,
                      service          : service,
                      provider         : provider,
                      messagePrefix    : messagePrefix,
                      messagesType     : messagesType,
                      extraInputCss    : extraInputCss,
                      fieldInputSize   : fieldInputSize,
                      hideBooleanLabel : hideBooleanLabel
            ]}"/>
</g:each>
<g:set var="defaultGroupName" value="${g.message(code:'plugin.property.secondary.groupName',default:'More')}"/>
<g:each in="${groupSet.keySet()}" var="group" status="n">
    <g:set var="groupProps" value="${groupSet[group]}"/>
    <g:set var="gkey" value="${g.rkey()}"/>
    <g:set var="hasValue" value="${values && groupProps*.name.find{name->values[name] && values[name]!= groupProps.find{it.name==name}?.defaultValue}}"/>
    <g:set var="isSecondary" value="${group in secondary}"/>

    <g:if test="${ungrouped || n>0}">
        <hr/>
    </g:if>

    <div class="" style="margin-top:10px;">
      <g:if test="${isSecondary}">
          <details ${hasValue ? 'open' : ''} class="details-reset more-info">
              <summary class="${groupTitleCss?:''}">
                  ${group != '-' ? group : defaultGroupName}
                  <span class="more-indicator-verbiage more-info-icon"><g:icon name="chevron-right"/></span>
                  <span class="less-indicator-verbiage more-info-icon"><g:icon name="chevron-down"/></span>
              </summary>

              <div>
                  <g:each in="${groupProps}" var="prop">
                      <g:render
                              template="/framework/pluginConfigPropertyFormField"
                              model="${[prop             : prop,
                                        dynamicProperties: dynamicProperties ? dynamicProperties[prop.name] : null,
                                        prefix           : prefix,
                                        error            : report?.errors ? report.errors[prop.name] : null,
                                        values           : values,
                                        fieldname        : (fieldnamePrefix ?: '') + prop.name,
                                        origfieldname    : (origfieldnamePrefix ?: '') + prop.name,
                                        service          : service,
                                        provider         : provider,
                                        messagePrefix    : messagePrefix,
                                        messagesType     : messagesType,
                                        extraInputCss    : extraInputCss,
                                        fieldInputSize   : fieldInputSize,
                                        hideBooleanLabel : hideBooleanLabel
                              ]}"/>
                  </g:each>
              </div>
          </details>

      </g:if>
      <g:else>
          <div class="${groupTitleCss?:''}">${group!='-'?group:defaultGroupName}</div>

          <div>
              <g:each in="${groupProps}" var="prop">
                  <g:render
                          template="/framework/pluginConfigPropertyFormField"
                          model="${[prop             : prop,
                                    dynamicProperties: dynamicProperties ? dynamicProperties[prop.name] : null,
                                    prefix           : prefix,
                                    error            : report?.errors ? report.errors[prop.name] : null,
                                    values           : values,
                                    fieldname        : (fieldnamePrefix ?: '') + prop.name,
                                    origfieldname    : (origfieldnamePrefix ?: '') + prop.name,
                                    service          : service,
                                    provider         : provider,
                                    messagePrefix    : messagePrefix,
                                    messagesType     : messagesType,
                                    extraInputCss    : extraInputCss,
                                    fieldInputSize   : fieldInputSize,
                                    hideBooleanLabel : hideBooleanLabel
                          ]}"/>
              </g:each>
          </div>
      </g:else>
    </div>
</g:each>

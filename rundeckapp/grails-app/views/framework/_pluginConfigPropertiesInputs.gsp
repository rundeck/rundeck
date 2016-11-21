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
<g:set var="groupSet" value="${[:]}"/>
<g:set var="secondary" value="${[]}"/>
<g:set var="ungrouped" value="${[]}"/>

<g:each in="${properties}" var="prop">
    <g:set var="scopeUnset" value="${!prop.scope || prop.scope.isUnspecified()}"/>
    <g:set var="scopeProject" value="${prop.scope && prop.scope.isProjectLevel()}"/>
    <g:set var="scopeInstance" value="${prop.scope && prop.scope.isInstanceLevel()}"/>
    <g:set var="scopeFramework" value="${prop.scope && prop.scope.isFrameworkLevel()}"/>

    <g:set var="scopeTest" value="${scopeUnset ||
            (allowedScope == PropertyScope.Instance && scopeInstance) ||
            (allowedScope == PropertyScope.Project && scopeProject) ||
            (allowedScope == PropertyScope.Framework && scopeFramework)
    }"/>
    <g:if test="${scopeTest}">
    %{--determine grouping--}%
        <g:if test="${prop.renderingOptions?.get(StringRenderingConstants.GROUPING)?.toString() == 'secondary'}">
            %{--secondary grouping--}%
            <g:set var="groupName" value="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)?.toString()?:'-'}"/>
            %{
                secondary<<groupName
            }%
            %{
                if(!groupSet[groupName]){
                    groupSet[groupName]=[prop]
                }else{
                    groupSet[groupName]<<prop
                }
            }%
        </g:if>
        <g:elseif test="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)}">
            %{--primary grouping--}%
            <g:set var="groupName" value="${prop.renderingOptions?.get(StringRenderingConstants.GROUP_NAME)?.toString()}"/>
            %{
                if(!groupSet[groupName]){
                    groupSet[groupName]=[prop]
                }else{
                    groupSet[groupName]<<prop
                }
            }%
        </g:elseif>
        <g:else>
            %{--no grouping--}%
            %{
            ungrouped<<prop
            }%
        </g:else>
    </g:if>
</g:each>
%{--Render ungrouped--}%
<g:each in="${ungrouped}" var="prop">
    <g:render
            template="/framework/pluginConfigPropertyFormField"
            model="${[prop         : prop,
                      prefix       : prefix,
                      error        : report?.errors ? report.errors[prop.name] : null,
                      values       : values,
                      fieldname    : (fieldnamePrefix ?: '') + prop.name,
                      origfieldname: (origfieldnamePrefix ?: '') + prop.name,
                      service      : service,
                      provider     : provider,
                      messagePrefix:messagePrefix
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

    <div class="form-group">
        <span class="col-sm-12 ">
            <g:if test="${isSecondary}">

                <g:collapser text="${group!='-'?group:defaultGroupName}"
                             key="propgroup_${gkey}"
                             open="${hasValue?'true':'false'}"
                             classnames=" control-label input-lg"
                />

            </g:if>
            <g:else>
                <span class="control-label input-lg">${group!='-'?group:defaultGroupName}</span>
            </g:else>
        </span>
    </div>

    <div id="propgroup_${gkey}" class="${wdgt.css(if:isSecondary,then:'collapse collapse-expandable')} ${wdgt.css(if:hasValue,then:'in')}">
        <g:each in="${groupProps}" var="prop">
            <g:render
                    template="/framework/pluginConfigPropertyFormField"
                    model="${[prop         : prop,
                              prefix       : prefix,
                              error        : report?.errors ? report.errors[prop.name] : null,
                              values       : values,
                              fieldname    : (fieldnamePrefix ?: '') + prop.name,
                              origfieldname: (origfieldnamePrefix ?: '') + prop.name,
                              service      : service,
                              provider     : provider,
                              messagePrefix:messagePrefix
                    ]}"/>
        </g:each>
    </div>
</g:each>
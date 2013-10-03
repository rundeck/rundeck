%{--
  - Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _pluginConfigProperty.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 12:01 PM
 --%>

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; com.dtolabs.rundeck.plugins.ServiceNameConstants; com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" contentType="text/html;charset=UTF-8" %>
%{--<g:set var="fieldname" value="${}"/>--}%
%{--<g:set var="origfieldname" value="${}"/>--}%
<g:set var="labelColType" value="col-sm-2 control-label input-sm"/>
<g:set var="valueColType" value="col-sm-10"/>
<g:set var="valueColTypeSplitA" value="col-sm-5"/>
<g:set var="valueColTypeSplitB" value="col-sm-5"/>
<g:set var="offsetColType" value="col-sm-10 col-sm-offset-2"/>
<g:set var="formControlType" value="form-control input-sm"/>
<g:set var="hasError" value="${error ? 'has-error' : ''}"/>
<g:set var="required" value="${prop.required ? 'required' : ''}"/>
<g:set var="propScope"
       value="${prop.scope != null && prop.scope != PropertyScope.Unspecified ? prop.scope : defaultScope}"/>
<g:unless test="${outofscopeOnly && propScope?.isInstanceLevel()}">
<div class="form-group ${hasError}">

<g:if test="${outofscope}">
    <div class="${labelColType} form-control-static ${error?'has-error':''}  ${prop.required ? 'required' : ''}">
        ${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}:
    </div>
    <div class="${valueColType}"  style="overflow-x:auto">

</g:if>
<g:elseif test="${prop.type.toString()=='Boolean'}">
    <g:set var="fieldid" value="${g.rkey()}"/>
    <div class="${offsetColType}">
        <g:hiddenField name="${origfieldname}" value="${values && values[prop.name] ? values[prop.name] : ''}"/>
        <div class="checkbox">
            <label
                   for="${fieldid.encodeAsHTML()}">${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}
                <g:checkBox name="${fieldname}" value="true"
                            checked="${values&&values[prop.name]?values[prop.name]=='true':prop.defaultValue=='true'}"
                            id="${fieldid}"/>
            </label>
        </div>
</g:elseif>
<g:elseif test="${prop.type.toString()=='Select' || prop.type.toString()=='FreeSelect'}">
    <g:set var="fieldid" value="${g.rkey()}"/>
    <label class="${labelColType}   ${prop.required ? 'required' : ''}"
           for="${fieldid.encodeAsHTML()}">${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}</label>

    <g:hiddenField name="${origfieldname}" value="${values&&values[prop.name]?values[prop.name]:''}"/>
    <g:if test="${prop.type.toString()=='FreeSelect'}">
        <div class="${valueColTypeSplitA}">
        <g:textField name="${fieldname}" value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                     id="${fieldid}" size="100" class="${formControlType}"/>
        </div>
        <div class="${valueColTypeSplitB}">
        <g:select name="${fieldid+'_sel'}" from="${prop.selectValues}" id="${fieldid}"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                  noSelection="['':'-choose a value-']"
            onchange="if(this.value){\$('${fieldid}').value=this.value;}"
            class="${formControlType}"
        />
        </div>
        <div class="${offsetColType}">
    </g:if>
    <g:elseif test="${prop.required}">
        <div class="${valueColType}">
        <g:select name="${fieldname}" from="${prop.selectValues}" id="${fieldid}"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                  class="${formControlType}"/>
    </g:elseif>
    <g:else>
        <div class="${valueColType}">
        <g:select name="${fieldname}" from="${prop.selectValues}" id="${fieldid}" noSelection="['':'-none selected-']"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                  class="${formControlType}"/>
    </g:else>
</g:elseif>
<g:else>
    <g:set var="fieldid" value="${g.rkey()}"/>
    <label class="${labelColType}  ${prop.required?'required':''}"
           for="${fieldid.encodeAsHTML()}" >${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}</label>
    <div class="${valueColType}">
    <g:hiddenField name="${origfieldname}" value="${values&&values[prop.name]?values[prop.name]:''}"/>
    <g:if test="${prop.renderingOptions?.('displayType') == StringRenderingConstants.DisplayType.MULTI_LINE}">
        <g:textArea name="${fieldname}" value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                 id="${fieldid}" rows="10" cols="100" class="${formControlType}"/>
    </g:if>
    <g:else>
        <g:textField name="${fieldname}" value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                 id="${fieldid}" size="100" class="${formControlType}"/>
    </g:else>
</g:else>
    <div class="help-block">${prop.description?.encodeAsHTML()}</div>
    <g:if test="${error}">
        <div class="text-warning">${error.encodeAsHTML()}</div>
    </g:if>
    <g:if test="${outofscope}">
        <g:render template="/framework/pluginConfigPropertyScopeInfo" model="[propScope:propScope,mapping:mapping, frameworkMapping: frameworkMapping, hideMissingFrameworkMapping: hideMissingFrameworkMapping]"/>
    </g:if>
</div>
</div>
</g:unless>

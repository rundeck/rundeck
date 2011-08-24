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

<%@ page contentType="text/html;charset=UTF-8" %>
%{--<g:set var="fieldname" value="${}"/>--}%
%{--<g:set var="origfieldname" value="${}"/>--}%

<g:if test="${prop.type.toString()=='Boolean'}">
    <g:set var="fieldid" value="${g.rkey()}"/>
    <td>
        <g:hiddenField name="${origfieldname}" value="${values&&values[prop.name]?values[prop.name]:''}"/>
    <g:checkBox name="${fieldname}" value="true" checked="${values&&values[prop.name]?values[prop.name]=='true':prop.defaultValue=='true'}" id="${fieldid}"/>
    </td>
    <td>
    <label class="${error ? 'fieldError' : ''}" for="${fieldid.encodeAsHTML()}">${prop.title? prop.title.encodeAsHTML(): prop.name.encodeAsHTML()}</label>
</g:if>
<g:elseif test="${prop.type.toString()=='Select' || prop.type.toString()=='FreeSelect'}">
    <g:set var="fieldid" value="${g.rkey()}"/>
    <td>
        <label class="${error ? 'fieldError' : ''}  ${prop.required ? 'required' : ''}" for="${fieldid.encodeAsHTML()}">${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}</label>:
    </td>
    <td>
    <g:hiddenField name="${origfieldname}" value="${values&&values[prop.name]?values[prop.name]:''}"/>
    <g:if test="${prop.type.toString()=='FreeSelect'}">
        <g:textField name="${fieldname}" value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                     id="${fieldid}" size="40"/>

        <g:select name="${fieldid+'_sel'}" from="${prop.selectValues}" id="${fieldid}"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                  noSelection="['':'-choose a value-']"
            onchange="if(this.value){\$('${fieldid}').value=this.value;}"
        />
    </g:if>
    <g:elseif test="${prop.required}">
        <g:select name="${fieldname}" from="${prop.selectValues}" id="${fieldid}"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"/>
    </g:elseif>
    <g:else>
        <g:select name="${fieldname}" from="${prop.selectValues}" id="${fieldid}" noSelection="['':'-none selected-']"
                  value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"/>

    </g:else>
</g:elseif>
<g:else>
    <g:set var="fieldid" value="${g.rkey()}"/>
    <td>
    <label class="${error ? 'fieldError' : ''} ${prop.required?'required':''}" for="${fieldid.encodeAsHTML()}" >${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}</label>:
    </td>
    <td>
    <g:hiddenField name="${origfieldname}" value="${values&&values[prop.name]?values[prop.name]:''}"/>
    <g:textField name="${fieldname}" value="${values&&null!=values[prop.name]?values[prop.name]:prop.defaultValue}"
                 id="${fieldid}" size="40"/>
</g:else>
    <div class="info note">${prop.description?.encodeAsHTML()}</div>
    <g:if test="${error}">
        <span class="warn note">${error.encodeAsHTML()}</span>
    </g:if>
</td>
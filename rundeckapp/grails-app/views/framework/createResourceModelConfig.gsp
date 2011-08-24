%{--
  - Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
    createResourceModelConfig.gsp.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 2:16 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<g:if test="${description}">
    <span class="prompt">${description.title.encodeAsHTML()}</span>
    <span class="info">${description.description.encodeAsHTML()}</span>
</g:if>
<div class="presentation">
    <g:if test="${error}">
        <span class="error note resourceConfigEdit">${error}</span>
    </g:if>
    <g:if test="${isCreate}">
        <g:hiddenField name="isCreate" value="true" class="isCreate"/>
    </g:if>
    <g:elseif test="${isEdit}">
        <g:hiddenField name="isEdit" value="true" class="isEdit"/>
    </g:elseif>
    <g:hiddenField name="prefix" value="${prefix}"/>
    <g:hiddenField name="${prefix+'type'}" value="${type}"/>
    <g:if test="${description}">
        <table class="simpleForm">
        <g:each in="${description.properties}" var="prop">
            <tr>
            <g:render template="pluginConfigPropertyField" model="${[prop:prop,prefix:prefix,error:report?.errors?report?.errors[prop.name]:null,values:values,fieldname:prefix+'config.'+prop.name,origfieldname:'orig.'+prefix+'config.'+prop.name]}"/>
            </tr>
        </g:each>
        </table>
    </g:if>
    <g:else>
        <span>Properties:</span>
        <ul>
        <g:each var="prop" in="${values}">
        <li>${prop.name.encodeAsHTML()}: ${prop.value.encodeAsHTML()} </li>
            <input type="hidden" name="${(prefix + 'config.' + prop.name).encodeAsHTML()}"
                   value="${prop.value?.encodeAsHTML()}"/>
        </g:each>
        </ul>
    </g:else>
</div>
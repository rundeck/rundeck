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
    _pluginConfigPropertyValue.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 12:03 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<td title="${prop.description?.encodeAsHTML()}">${prop.title ? prop.title.encodeAsHTML() : prop.name.encodeAsHTML()}:</td>
<td>
<g:if test="${prop.type.toString()=='Boolean'}">
    <g:if test="${values[prop.name]=='true'}">
        <span class="configvalue">Yes</span>
    </g:if>
    <g:else>
        No
    </g:else>
</g:if>
<g:elseif test="${values[prop.name]}">
    <span class="configvalue">${values[prop.name]?.encodeAsHTML()}</span>
</g:elseif>
<g:if test="${includeFormFields && values[prop.name]}">
    <input type="hidden" name="${(prefix+ 'config.'+prop.name).encodeAsHTML()}" value="${values[prop.name]?.encodeAsHTML()}"/>
</g:if>
</td>
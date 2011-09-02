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
    _renderServiceConfig.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 8/16/11 5:00 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<g:if test="${description}">
    <span class="prompt">${description.title.encodeAsHTML()}</span>
    <span class="desc">${description.description.encodeAsHTML()}</span>
</g:if>
<div class="" style="margin-top:5px;">
    <g:set var="rkey" value="${g.rkey()}"/><g:if test="${includeFormFields && saved}">
    <g:hiddenField name="${prefix}saved" value="true" class="wasSaved"/>
</g:if>
    <g:hiddenField name="prefix" value="${prefix}"/>
    <g:hiddenField name="${prefix+'type'}" value="${type}"/>
    <g:if test="${values}">
        <span id="${rkey}_summary">
            <g:if test="${description}">
                <g:each in="${description.properties}" var="prop">
                    <g:render template="/framework/pluginConfigPropertySummaryValue"
                              model="${[prop:prop,prefix:prefix,values:values,includeFormFields:includeFormFields]}"/>
                </g:each>
            </g:if>
        </span>
        <g:if test="${description}">
            <table class="simpleForm" id="${rkey}" style="display:none;">
                <g:each in="${description.properties}" var="prop">
                    <tr>
                        <g:render template="/framework/pluginConfigPropertyValue"
                                  model="${[prop:prop,prefix:prefix,values:values,includeFormFields:includeFormFields]}"/>
                    </tr>
                </g:each>
            </table>
        </g:if>
        <g:elseif test="${includeFormFields}">
            <g:expander key="${rkey}_inv">Properties</g:expander>
            <ul id="${rkey}_inv" style="display:none">
                <g:each var="prop" in="${values}">
                    <li>${prop?.key.encodeAsHTML()}: ${prop?.value.encodeAsHTML()}</li>
                    <input type="hidden" name="${(prefix + 'config.' + prop?.key).encodeAsHTML()}"
                           value="${prop?.value?.encodeAsHTML()}"/>
                </g:each>
            </ul>
        </g:elseif>

    </g:if>

</div>
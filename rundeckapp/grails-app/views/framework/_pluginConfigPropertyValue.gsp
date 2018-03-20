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
 <%--
    _pluginConfigPropertyValue.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 12:03 PM
 --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<g:set var="propValue" value="${values[prop.name]}"/>
<g:if test="${includeFormFields && propValue}">

    <g:if test="${prop.type.toString() in ['Options'] && propValue instanceof Collection}">
        <g:each in="${propValue}" var="val">
            <input type="hidden" name="${enc(attr: prefix + 'config.' + prop.name)}" value="${val}"/>
        </g:each>
    </g:if>
    <g:elseif test="${prop.type.toString() in ['Map'] && propValue instanceof Map}">
        <input type="hidden" name="${enc(attr: "${prefix}config.${prop.name}.${i}._type")}" value="map"/>
        <g:each in="${propValue.keySet()}" var="keyVal" status="i">
            <input type="hidden" name="${enc(attr: "${prefix}config.${prop.name}.${i}.key")}" value="${keyVal}"/>
            <input type="hidden" name="${enc(attr: "${prefix}config.${prop.name}.${i}.value")}"
                   value="${propValue[keyVal]}"/>
        </g:each>
    </g:elseif>
    <g:else>
        <input type="hidden" name="${enc(attr: prefix + 'config.' + prop.name)}" value="${propValue}"/>
    </g:else>
</g:if>

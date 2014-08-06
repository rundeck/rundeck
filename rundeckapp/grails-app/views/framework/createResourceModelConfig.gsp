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
<div class="container">


<g:if test="${description}">
    <div class="row">
    <div class="col-sm-12">
    <div class="h4 text-info">${enc(html:description.title)}
        <small class="text-muted">${enc(html:description.description)}</small>
    </div>

    </div>
    </div>
</g:if>
<div class="row ${description?'row-space':''}">
    <g:if test="${error}">
        <div class="col-sm-12">
        <div class="alert alert-warning resourceConfigEdit">${error}</div>
        </div>
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
        <div class="col-sm-12 form-horizontal">
        <g:each in="${description.properties}" var="prop">
            <g:render template="pluginConfigPropertyFormField" model="${[prop:prop,prefix:prefix,error:report?.errors?report?.errors[prop.name]:null,values:values,fieldname:prefix+'config.'+prop.name,origfieldname:'orig.'+prefix+'config.'+prop.name]}"/>
        </g:each>
        </div>
    </g:if>
    <g:else>
        <div class="col-sm-12">
        <span>Properties:</span>
        <ul>
        <g:each var="prop" in="${values}">
        <li>${enc(html:prop.name)}: ${enc(html:prop.value)} </li>
            <input type="hidden" name="${enc(html:prefix + 'config.' + prop.name)}"
                   value="${enc(html:prop.value)}"/>
        </g:each>
        </ul>
        </div>
    </g:else>
</div>

</div>

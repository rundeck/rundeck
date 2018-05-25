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
    createResourceModelConfig.gsp.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 7/28/11 2:16 PM
 --%>

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %>
<div class="container">


<g:if test="${description}">
    <div class="row">
    <div class="col-sm-12">
    <div class="h4 text-info"><g:enc>${description.title}</g:enc>
        <small class="text-muted"><g:enc>${description.description}</g:enc></small>
    </div>

    </div>
    </div>
</g:if>
<div class="row ${description?'row-space':''}">
    <g:if test="${error}">
        <div class="col-sm-12">
        <div class="alert alert-warning resourceConfigEdit"><g:enc>${error}</g:enc></div>
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

            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                    service:com.dtolabs.rundeck.plugins.ServiceNameConstants.ResourceModelSource,
                    provider:description.name,
                    properties:description.properties,
                    report:report,
                    prefix:prefix,
                    values:values,
                    fieldnamePrefix:prefix+'config.',
                    origfieldnamePrefix:'orig.'+prefix+'config.',
                    allowedScope: PropertyScope.Project
            ]}"/>
        </div>
    </g:if>
    <g:else>
        <div class="col-sm-12">
        <span>Properties:</span>
        <ul>
        <g:each var="prop" in="${values}">
        <li><g:enc>${prop.name}: ${prop.value}</g:enc> </li>
            <input type="hidden" name="${enc(attr:prefix + 'config.' + prop.name)}"
                   value="${enc(attr:prop.value)}"/>
        </g:each>
        </ul>
        </div>
    </g:else>
</div>

</div>

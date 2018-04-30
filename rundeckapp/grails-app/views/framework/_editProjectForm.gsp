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
   _editProjectForm.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 8/1/11 11:38 AM
--%>

<%@ page import="com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %>

<div class="list-group">
<g:if test="${editOnly}">
    <g:hiddenField name="project" value="${project}"/>
</g:if>
    <div class="list-group-item">
    <g:if test="${!editOnly}">
        <div class="form-group ${projectNameError?'has-error':''}">
            <label for="project" class="required">
                <g:message code="domain.Project.field.name" default="Project Name"/>
            </label>
            <g:textField name="newproject" size="50" autofocus="true" value="${newproject}" class="form-control"/>

            <g:if test="${projectNameError}">
                <div class="text-warning"><g:enc>${projectNameError}</g:enc></div>
            </g:if>
        </div>
    </g:if>
    <g:render template="/common/messages" model="[notDismissable:true]"/>
</div>
    <div class="list-group-item">
        <div class="form-group ">
            <label for="label">
                <g:message code="domain.Project.label.label" default="Label"/>
            </label>
            <g:textField name="label" size="50"  value="${projectLabel}" class="form-control"/>
        </div>
        <div class="form-group ">
            <label for="description">
                <g:message code="domain.Project.description.label" default="Description"/>
            </label>
            <g:textField name="description" size="50"  value="${projectDescription}" class="form-control"/>
        </div>
    </div>
    <g:set var="categories"
           value="${new HashSet(extraConfig?.values()?.collect { it.configurable.categories?.values() }.flatten())}"/>

    <g:each in="${categories.sort() - 'resourceModelSource'}" var="category">


        <g:render template="projectConfigurableForm"
                  model="${[extraConfigSet: extraConfig?.values(),
                            category      : category,
                            categoryPrefix     : 'extra.category.' + category + '.',
                            titleCode     : 'project.configuration.extra.category.' + category + '.title',
                            helpCode      : 'project.configuration.extra.category.' + category + '.description'
                  ]}"/>

    </g:each>

<g:if test="${nodeExecDescriptions}">
    <div class="list-group-item">
    <span class="h4">Default <g:message code="framework.service.NodeExecutor.label" /></span>


        <span class="help-block"><g:message code="domain.Project.edit.NodeExecutor.explanation" /></span>
        <g:each in="${nodeExecDescriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="isSelected" value="${defaultNodeExec == description.name}"/>
            <div class="radio">
                <label>
                    <g:radio
                        name="defaultNodeExec"
                        value="${nex}"
                        class="nexec"
                        id="${nkey+'_input'}"
                        checked="${isSelected}"/>
                    <b><g:enc>${description.title}</g:enc></b>
                </label>
                <span class="help-block"><g:enc>${description.description}</g:enc></span>
            </div>
                <g:hiddenField name="nodeexec.${nex}.type" value="${description.name}"/>
                <g:set var="nodeexecprefix" value="nodeexec.${nex}.config."/>
                <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                    <wdgt:action visible="false" targetSelector=".nexecDetails"/>
                </wdgt:eventHandler>
                <g:if test="${description && description.properties}">
                    <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                        <wdgt:action visible="true" target="${nkey+'_det'}"/>
                    </wdgt:eventHandler>
                    <div class="well well-sm nexecDetails" id="${enc(attr:nkey) + '_det'}"
                         style="${wdgt.styleVisible(if: isSelected)}">
                        <div class="form-horizontal " >
                            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                    service:com.dtolabs.rundeck.plugins.ServiceNameConstants.NodeExecutor,
                                    provider:description.name,
                                    properties:description.properties,
                                    report:nodeexecreport?.errors && isSelected ? nodeexecreport : null,
                                    prefix:nodeexecprefix,
                                    values:isSelected ? nodeexecconfig : null,
                                    fieldnamePrefix:nodeexecprefix,
                                    origfieldnamePrefix:'orig.' + nodeexecprefix,
                                    allowedScope: PropertyScope.Project
                            ]}"/>
                    </div>
                    </div>
                </g:if>
        </g:each>
    </div>
</g:if>
<g:if test="${fileCopyDescriptions}">
    <div class="list-group-item">
    <span class="h4">Default Node <g:message code="framework.service.FileCopier.label"/></span>


        <span class="help-block"><g:message code="domain.Project.edit.FileCopier.explanation" /></span>
        <g:each in="${fileCopyDescriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="isSelected" value="${defaultFileCopy == description.name}"/>
            <div class="radio">
                <label>
                    <g:radio
                        name="defaultFileCopy"
                        value="${nex}"
                        class="fcopy"
                        id="${nkey+'_input'}"
                        checked="${isSelected}"/>
                    <b><g:enc>${description.title}</g:enc></b>
                </label>
                <span class="help-block"><g:enc>${description.description}</g:enc></span>
            </div>
            <g:hiddenField name="fcopy.${nex}.type" value="${description.name}"/>
            <g:set var="fcopyprefix" value="fcopy.${nex}.config."/>
            <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                <wdgt:action visible="false" targetSelector=".fcopyDetails"/>
            </wdgt:eventHandler>
            <g:if test="${description && description.properties}">
                <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
                    <wdgt:action visible="true" target="${nkey+'_det'}"/>
                </wdgt:eventHandler>
                <div class="well well-sm fcopyDetails" id="${enc(attr:nkey) + '_det'}"
                       style="${wdgt.styleVisible(if: isSelected)}">
                <div class="form-horizontal " >

                    <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                            service:com.dtolabs.rundeck.plugins.ServiceNameConstants.FileCopier,
                            provider:description.name,
                            properties:description.properties,
                            report:fcopyreport?.errors && isSelected ? fcopyreport : null,
                            prefix:fcopyprefix,
                            values:isSelected?fcopyconfig:null,
                            fieldnamePrefix:fcopyprefix,
                            origfieldnamePrefix:'orig.'+fcopyprefix,
                            allowedScope:PropertyScope.Project
                    ]}"/>
                    %{--<g:each in="${description.properties}" var="prop">--}%
                        %{--<g:if test="${!prop.scope || prop.scope.isProjectLevel() || prop.scope.isUnspecified()}">--}%
                            %{--<g:render--}%
                                %{--template="pluginConfigPropertyFormField"--}%
                                %{--model="${[prop:prop,prefix:fcopyprefix,error:fcopyreport?.errors && isSelected ?fcopyreport?.errors[prop.name]:null,--}%
                                          %{--values: isSelected?fcopyconfig:null,--}%
                            %{--fieldname:fcopyprefix+prop.name,origfieldname:'orig.'+fcopyprefix+prop.name]}"/>--}%
                        %{--</g:if>--}%
                    %{--</g:each>--}%
                </div>
                </div>
            </g:if>
        </g:each>
    </div>
</g:if>
</div>

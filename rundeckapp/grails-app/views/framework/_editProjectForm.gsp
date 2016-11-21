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
            <label for="description">
                <g:message code="domain.Project.description.label" default="Description"/>
            </label>
            <g:textField name="description" size="50"  value="${projectDescription}" class="form-control"/>
        </div>
    </div>
<g:if test="${resourceModelConfigDescriptions}">
    <div class="list-group-item">
    <span class="h4 ">
        <g:message code="framework.service.ResourceModelSource.label" />
    </span>

    <div class="help-block">
        <g:message code="domain.Project.edit.ResourceModelSource.explanation" />
    </div>

    <div class="alert alert-warning" id="errors" style="display:none;">

    </div>
    <ol id="configs" >
        <g:if test="${configs}">
            <g:each var="config" in="${configs}" status="n">
                <li>
                    <div class="inpageconfig">
                        <g:set var="desc" value="${resourceModelConfigDescriptions.find {it.name==config.type}}"/>
                        <g:if test="${!desc}">
                            <span
                                class="warn note invalidProvider">Invalid Resource Model Source configuration: Provider not found: <g:enc>${config.type}</g:enc></span>
                        </g:if>
                        <g:render template="viewResourceModelConfig"
                                  model="${[prefix: prefixKey+'.'+(n+1)+'.', values: config.props, includeFormFields: true, description: desc, saved:true,type:config.type]}"/>
                    </div>
                </li>
            </g:each>
        </g:if>
    </ol>

    <div id="sourcebutton" >
        <button class="btn btn-success btn-sm">
            Add Source
            <i class="glyphicon glyphicon-plus"></i>
        </button>
    </div>

    <div id="sourcepicker" class="panel panel-success sourcechrome" style="display:none;">
        <div class="panel-heading">
            <g:message code="framework.service.ResourceModelSource.add.title"/>
        </div>
        <div class="list-group">
            <g:each in="${resourceModelConfigDescriptions}" var="description">
                <a onclick="configControl.addConfig('${enc(js: description.name)}');
                return false;"
                    href="#"
                   class="list-group-item">
                    <strong>
                        <i class="glyphicon glyphicon-plus"></i>
                        <g:enc>${description.title}</g:enc>
                    </strong>
                    <span class="help-block"><g:enc>${description.description}</g:enc></span>
                </a>
            </g:each>
        </div>

        <div id="sourcecancel" class="panel-footer">
            <button class="btn btn-default btn-sm">Cancel</button>
        </div>

    </div>
    </div>

</g:if>

    <g:if test="${extraConfig}">
       <div class="list-group-item">
           <span class="h4 ">
                <g:message code="resource.model" />
            </span>

            <div class="help-block">
                <g:message code="additional.configuration.for.the.resource.model.for.this.project" />
            </div>

            <div class="form-horizontal">
                <g:each in="${extraConfig.keySet()}" var="configService">
                    <g:set var="configurable" value="${extraConfig[configService].configurable}"/>
                    <g:if test="${configurable.category == 'resourceModelSource'}">

                        <g:set var="pluginprefix" value="${extraConfig[configService].get('prefix')}"/>
                        <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                properties:configurable.projectConfigProperties,
                                report:extraConfig[configService].get('report'),
                                prefix:pluginprefix,
                                values:extraConfig[configService].get('values')?:[:],
                                fieldnamePrefix:pluginprefix,
                                origfieldnamePrefix:'orig.' + pluginprefix,
                                allowedScope:PropertyScope.Project
                        ]}"/>
                    </g:if>
                </g:each>
            </div>
       </div>
    </g:if>

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

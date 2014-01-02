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
   _editProjectForm.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 8/1/11 11:38 AM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

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
                <div class="text-warning">${projectNameError.encodeAsHTML()}</div>
            </g:if>
        </div>
    </g:if>


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
                                class="warn note invalidProvider">Invalid Resurce Model Source configuration: Provider not found: ${config.type.encodeAsHTML()}</span>
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
                <a onclick="configControl.addConfig('${description.name.encodeAsJavaScript()}');
                return false;"
                    href="#"
                   class="list-group-item">
                    <strong>
                        <i class="glyphicon glyphicon-plus"></i>
                        ${description.title.encodeAsHTML()}
                    </strong>
                    <span class="help-block">${description.description.encodeAsHTML()}</span>
                </a>
            </g:each>
        </div>

        <div id="sourcecancel" class="panel-footer">
            <button class="btn btn-default btn-sm">Cancel</button>
        </div>

    </div>
    </li>
    </div>

</g:if>

<g:if test="${nodeExecDescriptions}">
    <div class="list-group-item">
    <span class="h4">Default <g:message code="framework.service.NodeExecutor.label" /></span>


        <span class="help-block"><g:message code="domain.Project.edit.NodeExecutor.explanation" /></span>
        <g:each in="${nodeExecDescriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <div class="radio">
                <label>
                    <g:radio
                        name="defaultNodeExec"
                        value="${nex}"
                        class="nexec"
                        id="${nkey+'_input'}"
                        checked="${defaultNodeExec?defaultNodeExec==description.name:false}"/>
                    <b>${description.title.encodeAsHTML()}</b>
                </label>
                <span class="help-block">${description.description.encodeAsHTML()}</span>
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
                    <div class="well well-sm nexecDetails" id="${nkey + '_det'}"
                         style="${wdgt.styleVisible(if: defaultNodeExec == description.name)}">
                        <div class="form-horizontal " >
                        <g:each in="${description.properties}" var="prop">
                                <g:render
                                    template="pluginConfigPropertyFormField"
                                    model="${[prop:prop,prefix:nodeexecprefix,error:nodeexecreport?.errors?nodeexecreport?.errors[prop.name]:null,values:nodeexecconfig,
                                fieldname:nodeexecprefix+prop.name,origfieldname:'orig.'+nodeexecprefix+prop.name]}"/>
                        </g:each>
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
            <div class="radio">
                <label>
                    <g:radio
                        name="defaultFileCopy"
                        value="${nex}"
                        class="fcopy"
                        id="${nkey+'_input'}"
                        checked="${defaultFileCopy?defaultFileCopy==description.name:false}"/>
                    <b>${description.title.encodeAsHTML()}</b>
                </label>
                <span class="help-block">${description.description.encodeAsHTML()}</span>
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
                <div class="well well-sm fcopyDetails" id="${nkey + '_det'}"
                       style="${wdgt.styleVisible(if: defaultFileCopy == description.name)}">
                <div class="form-horizontal " >
                    <g:each in="${description.properties}" var="prop">
                            <g:render
                                template="pluginConfigPropertyFormField"
                                model="${[prop:prop,prefix:fcopyprefix,error:fcopyreport?.errors?fcopyreport?.errors[prop.name]:null,values:fcopyconfig,
                            fieldname:fcopyprefix+prop.name,origfieldname:'orig.'+fcopyprefix+prop.name]}"/>
                    </g:each>
                </div>
                </div>
            </g:if>
        </g:each>
    </div>
</g:if>
</div>

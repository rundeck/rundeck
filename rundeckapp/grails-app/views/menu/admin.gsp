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
   admin.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 6/1/11 2:22 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title>Configure</title>
</head>

<body>

<div class="row">
<div class="col-sm-12">
    <g:render template="/common/messages"/>
    <g:if test="${flash.joberrors}">
        <ul class="error note">
            <g:each in="${flash.joberrors}" var="errmsg">
                <li>${errmsg.encodeAsHTML()}</li>
            </g:each>
        </ul>
    </g:if>
    </div>
</div>
<div class="row">
<div class="col-sm-3">
    <g:render template="configNav" model="[selected:'project']"/>
</div>
<div class="col-sm-9">

    <span class="h3">
        Project: ${(params.project ?: request.project).encodeAsHTML()}

    </span>
<div class="row row-space">
<div class="col-sm-12">
    <ul class="nav nav-tabs">
        <li class="active">
            <a href="#configure"
                data-toggle="tab"
                >Configuration</a>
        </li>
        <li>
            <a href="#export"
               data-toggle="tab"
                >Export Archive</a>
        </li>
        <li>
            <a href="#import"
               data-toggle="tab"
                >Import Archive</a>
        </li>
    </ul>

<div class="tab-content">
<div class="tab-pane active" id="configure">
<ul class="list-group list-group-tab-content">
        <g:link controller="framework" action="editProject" params="[project: params.project ?: request.project]"
                class="textbtn textbtn-info list-group-item  textbtn-on-hover">
            <i class="glyphicon glyphicon-edit"></i>
            <g:message code="gui.menu.ProjectEdit" default="edit configuration"/>
        </g:link>


        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                <g:message code="framework.service.ResourceModelSource.label"/>
            </h4>
            <g:if test="${!configs}">
                <div class="text-muted"><g:message code="message.none.set"/></div>
            </g:if>

            <ol id="configs">
                <g:if test="${configs}">
                    <g:each var="config" in="${configs}" status="n">
                        <li>
                            <div class="inpageconfig">
                                <g:set var="desc"
                                       value="${resourceModelConfigDescriptions.find {it.name==config.type}}"/>
                                <g:if test="${desc}">

                                    <g:render template="/framework/viewResourceModelConfig"
                                              model="${[values: config.props, description: desc]}"/>
                                </g:if>
                                <g:else>
                                    <span
                                        class="warn note">Invalid Resource Model Source configuration: Provider not found: ${config.type.encodeAsHTML()}</span>
                                </g:else>
                            </div>
                        </li>
                    </g:each>
                </g:if>
            </ol>
        </li>

        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                Default <g:message code="framework.service.NodeExecutor.label"/>
            </h4>
            <span class="text-muted"><g:message code="domain.Project.edit.NodeExecutor.explanation"/></span>
            <g:if test="${!nodeexecconfig}">
                <div class="text-warning"><g:message code="message.none.set"/></div>
            </g:if>

            <g:if test="${nodeexecconfig}">
                <div class="inpageconfig">
                    <g:set var="desc" value="${nodeExecDescriptions.find {it.name==nodeexecconfig.type}}"/>
                    <g:if test="${desc}">

                        <g:render template="/framework/renderPluginConfig"
                                  model="${[values: nodeexecconfig.config, description: desc]}"/>
                    </g:if>
                    <g:else>
                        <span
                            class="warn note"><g:message code="framework.service.error.missing-provider"
                                                         args="[nodeexecconfig.type]"/></span>
                    </g:else>
                </div>
            </g:if>
        </li>

        <li class="list-group-item">
            <h4 class="list-group-item-heading">
                Default <g:message code="framework.service.FileCopier.label"/>
            </h4>
            <span
                class="text-muted"><g:message code="domain.Project.edit.FileCopier.explanation"/></span>
            <g:if test="${!fcopyconfig}">
                <div class="text-warning"><g:message code="message.none.set"/></div>
            </g:if>

            <g:if test="${fcopyconfig}">
                <div class="inpageconfig">
                    <g:set var="desc" value="${fileCopyDescriptions.find {it.name==fcopyconfig.type}}"/>
                    <g:if test="${desc}">

                        <g:render template="/framework/renderPluginConfig"
                                  model="${[values: fcopyconfig.config, description: desc]}"/>
                    </g:if>
                    <g:else>
                        <span
                            class="warn note"><g:message code="framework.service.error.missing-provider"
                                                         args="[fcopyconfig.type]"/></span>
                    </g:else>
                </div>
            </g:if>
        </li>
    </ul>
</div>

<div class="tab-pane" id="export">
    <div class="panel panel-default panel-tab-content">
        <div class="panel-heading">
            Download an archive of project <strong>${(params.project ?: request.project).encodeAsHTML()}</strong>
        </div>
        <div class="panel-body">
                <g:link controller="project" action="export" params="[project: params.project ?: request.project]"
                    class="btn btn-success"
                >
                    <i class="glyphicon glyphicon-download-alt"></i>
                    ${(params.project ?: request.project).encodeAsHTML()}.rdproject.jar
                </g:link>

        </div>

    </div>
</div>

<div class="tab-pane" id="import">
    <g:form controller="project" action="importArchive" params="[project:params.project ?: request.project]"
            enctype="multipart/form-data" class="form">
    <div class="list-group list-group-tab-content">
        <div class="list-group-item">
            <div class="form-group">
                <label>
                    Choose a Rundeck archive
                    <input type="file" name="zipFile" class="form-control"/>
                </label>

                <p class="help-block">
                    Existing Jobs in this project that match imported Jobs (group and name match, or UUID matches) will be updated.
                </p>
            </div>
        </div>

        <div class="list-group-item">
            <h4 class="list-group-item-heading">Imported Jobs</h4>

            <div class="radio">
                    <label title="Original UUIDs will be preserved, conflicting UUIDs will be replaced">
                        <input type="radio" name="import.jobUUIDBehavior" value="preserve" checked />
                        <g:message code="project.archive.import.jobUUIDBehavior.preserve.label"/>
                    </label>

                <p class="help-block"><g:message
                        code="project.archive.import.jobUUIDBehavior.preserve.description"/></p>
            </div>
            <div class="radio">
                <label title="New UUIDs will be generated for every imported Job">
                    <input type="radio" name="import.jobUUIDBehavior" value="remove"/>
                    <g:message code="project.archive.import.jobUUIDBehavior.remove.label"/>
                </label>

                <p class="help-block"><g:message
                        code="project.archive.import.jobUUIDBehavior.remove.description"/></p>
            </div>
        </div>

        <div class="list-group-item">
            <h4 class="list-group-item-heading">Executions</h4>

            <div class="radio">
                <label title="All executions and reports will be imported">
                    <input type="radio" name="import.executionImportBehavior" value="import" checked/>
                    Import All
                </label>
                <span class="help-block">Creates new Executions and History reports from the archive</span>
            </div>

            <div class="radio">
                <label title="No executions or reports will be imported">
                    <input type="radio" name="import.executionImportBehavior" value="skip"/>
                    Do Not Import
                </label>
                <span class="help-block">Does not import any Executions or History</span>
            </div>
        </div>
        <div class="list-group-item">
            <div class="buttons">
                <div id="uploadFormButtons">
                    <g:actionSubmit id="createFormCancelButton" value="Cancel" class="btn btn-default"/>
                    <g:actionSubmit action="importArchive" value="Import" id="uploadFormUpload"
                                    onclick="['uploadFormButtons','importUploadSpinner'].each(Element.toggle)"
                                    class="btn btn-primary"/>
                </div>

                <div id="importUploadSpinner" class="spinner block" style="display:none;">
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                         alt="Spinner"/>
                    Uploading File...
                </div>
            </div>
        </div>
        </div>
    </div>
    </g:form>
</div>
</div>
</div>
</div>
</div>
</body>
</html>

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

<div class="pageBody">
    <g:render template="/common/messages"/>
    <g:if test="${flash.joberrors}">
        <ul class="error note">
            <g:each in="${flash.joberrors}" var="errmsg">
                <li>${errmsg.encodeAsHTML()}</li>
            </g:each>
        </ul>
    </g:if>

    <ul>
        <li>
            <g:link controller="menu" action="systemInfo">
                <g:message code="gui.menu.SystemInfo" default="System Information"/>
            </g:link>
        </li>
        <li>
            <g:link controller="user" action="list">
                <g:message code="gui.menu.UserProfiles" default="User Profiles"/>
            </g:link>
        </li>
    </ul>

    <div class="rounded" style="width:600px;">
    <ul>
        <li>
            <g:set var="pluginParams"
                   value="${[utm_source: 'rundeckapp', utm_medium: 'app', utm_campaign: 'getpluginlink'].collect { k, v -> k + '=' + v }.join('&')}"/>

            <g:set var="pluginUrl" value="http://rundeck.org/plugins/?${pluginParams}"/>
            <g:set var="pluginLinkUrl"
                   value="${org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.pluginLink ?: pluginUrl}"/>
            <a href="${pluginLinkUrl}">
                <g:message code="gui.admin.GetPlugins" default="Get Plugins"/>
            </a>
        </li>
    </ul>
    </div>

    <div class="rounded" style="width:600px;">
        Project: <span class="prompt">${session.project.encodeAsHTML()}</span>
        -
        <g:link controller="framework" action="editProject" params="[project:session.project]" class="action textbtn">
            <g:message code="gui.menu.ProjectEdit" default="Configure Project"/>
        </g:link>

        <div class="presentation ">
            <table class="simpleform">

                <tr>
                    <td><g:message code="domain.Project.field.resourcesUrl" default="Resources Provider URL"/>:</td>
                    <td>
                        <g:if test="${resourcesUrl}">
                            <span class="configvalue">${resourcesUrl.encodeAsHTML()}</span>
                        </g:if>
                        <g:else>
                            <span class="info note"><g:message code="message.none.set"/></span>
                        </g:else>
                    </td>
                </tr>
                <tr>
                    <td><g:message code="domain.Project.field.sshKeyPath" default="Default SSH Key File"/>:</td>
                    <td>
                        <g:if test="${sshkeypath}">
                            <span class="configvalue">${sshkeypath.encodeAsHTML()}</span>
                        </g:if>
                        <g:else>
                            <span class="info note"><g:message code="message.none.set"/></span>
                        </g:else>
                    </td>
                </tr>
            </table>
        </div>

        <span class="prompt section">Export Archive</span>

        <div class="presentation">
            <g:link controller="project" action="export" params="[name: session.project]">
                <img src="${resource(dir: 'images', file: 'icon-small-file.png')}" alt="download" width="13px"
                     height="16px"/>
                ${session.project.encodeAsHTML()}.rdproject.jar
            </g:link>
            -
            <span class="info note">
                Download an archive of project <em>${session.project.encodeAsHTML()}</em>
            </span>
        </div>

        <g:expander key="projectImport" classnames="prompt section">Import Archive</g:expander>
        <div style="display:none" id="projectImport" class="presentation ">
            <g:form controller="project" action="importArchive" enctype="multipart/form-data">
                <div>
                    <label>
                        Choose a Rundeck archive
                        <input type="file" name="zipFile"/>
                    </label>
                </div>

                <ul>
                    <li>Existing Jobs in this project that match imported Jobs (group and name match, or UUID matches) will be updated.</li>
                </ul>
                <span class="prompt">New Jobs</span>
                <div class="presentation">
                    <div>
                        <label title="Original UUIDs will be preserved, conflicting UUIDs will be replaced">
                            <input type="radio" name="import.jobUUIDBehavior" value="preserve" checked/>
                            <g:message code="project.archive.import.jobUUIDBehavior.preserve.label" />
                        </label>
                        <span class="info note"><g:message code="project.archive.import.jobUUIDBehavior.preserve.description" /></span>
                    </div>

                    <div>
                        <label title="New UUIDs will be generated for every imported Job">
                            <input type="radio" name="import.jobUUIDBehavior" value="remove"/>
                            <g:message code="project.archive.import.jobUUIDBehavior.remove.label" />
                        </label>
                        <span class="info note"><g:message code="project.archive.import.jobUUIDBehavior.remove.description" /></span>
                    </div>
                </div>

                <span class="prompt">Executions</span>

                <div class="presentation">
                    <div>
                        <label title="All executions and reports will be imported">
                            <input type="radio" name="import.executionImportBehavior" value="import" checked/>
                            Import All
                        </label>
                        <span class="info note">Creates new Executions and History reports from the archive</span>
                    </div>

                    <div>
                        <label title="No executions or reports will be imported">
                            <input type="radio" name="import.executionImportBehavior" value="skip"/>
                            Do Not Import
                        </label>
                        <span class="info note">Does not import any Executions or History</span>
                    </div>
                </div>

                <g:hiddenField name="name" value="${session.project}"/>

                <div class="buttons">
                    <div id="uploadFormButtons">
                        <g:actionSubmit id="createFormCancelButton" value="Cancel"/>
                        <g:actionSubmit action="importArchive" value="Import" id="uploadFormUpload"
                                        onclick="['uploadFormButtons','importUploadSpinner'].each(Element.toggle)"/>
                    </div>

                    <div id="importUploadSpinner" class="spinner block" style="display:none;">
                        <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}"
                             alt="Spinner"/>
                        Uploading File...
                    </div>
                </div>
            </g:form>
        </div>

        <span class="prompt section">
            <g:message code="framework.service.ResourceModelSource.label"/>
        </span>

        <div class="presentation">
            <g:if test="${!configs}">
                <span class="info note"><g:message code="message.none.set"/></span>
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
                                        class="warn note">Invalid Resurce Model Source configuration: Provider not found: ${config.type.encodeAsHTML()}</span>
                                </g:else>
                            </div>
                        </li>
                    </g:each>
                </g:if>
            </ol>
        </div>
        <span class="prompt section">
            Default <g:message code="framework.service.NodeExecutor.label"/>
        </span>

        <div class="presentation">
            <span
                class="info note"><g:message code="domain.Project.edit.NodeExecutor.explanation"/></span>
            <g:if test="${!nodeexecconfig}">
                <span class="info note"><g:message code="message.none.set"/></span>
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
        </div>
        <span class="prompt section">
            Default <g:message code="framework.service.FileCopier.label"/>
        </span>

        <div class="presentation">
            <span
                class="info note"><g:message code="domain.Project.edit.FileCopier.explanation"/></span>
            <g:if test="${!fcopyconfig}">
                <span class="info note"><g:message code="message.none.set" /></span>
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
        </div>
    </div>
</div>
</body>
</html>

%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:set var="authAdmin" value="${auth.resourceAllowedTest(
        action: AuthConstants.ACTION_ADMIN,
        type: "project",
        name: (params.project ?: request.project),
        context: "application"
)}"/>
<g:set var="authDelete"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_DELETE, type: "project",
                                                      name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authExport"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_EXPORT, type: "project",
                                                      name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authImport"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_IMPORT, type: "project",
                                                      name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authConfigure"
       value="${authAdmin || auth.resourceAllowedTest(action: AuthConstants.ACTION_CONFIGURE, type: "project",
                                                      name: (params.project ?: request.project), context: "application"
       )}"/>

<ul class="dropdown-menu" role="menu" aria-labelledby="">

    <li class="dropdown-header"><g:message code="project.admin" /></li>

    <li class="${!authConfigure ? 'disabled' : ''}">
        <g:link controller="framework" action="editProject" params="[project: params.project ?: request.project]">
            <g:message code="edit.configuration"/>
        </g:link>
    </li>
    <li>
        <g:link controller="menu"
                action="projectAcls"
                params="[project: params.project ?: request.project]">
            <g:message code="list.acls" />
        </g:link>
    </li>
    <li role="separator" class="divider"></li>
    <li>
        <g:link controller="framework"
                action="editProjectFile"
                params="[project: params.project ?: request.project, filename: 'readme.md']">
            <g:message code="edit.readme"/>
        </g:link>
    </li>

    <li>
        <g:link
                controller="framework"
                action="editProjectFile"
                params="[project: params.project ?: request.project, filename: 'motd.md']">
            <g:message code="edit.message.of.the.day"/>
        </g:link>
    </li>
    <li role="separator" class="divider"></li>
    <li class="${!authConfigure ? 'disabled' : ''}">
        <g:link controller="scm" action="index"
                params="[project: params.project ?: request.project]">
            <g:message code="project.admin.menu.Scm.title" default="Setup SCM"/>
        </g:link>
    </li>
    <li role="separator" class="divider"></li>
    <li class="${!authExport ? 'disabled' : ''}">
        <g:link
                controller="menu"
                action="projectExport"
                params="[project: params.project ?: request.project]"
                title="${authExport ? '' : message(code: "request.error.unauthorized.title")}">
            <g:message code="export.archive.ellipsis" />
        </g:link>
    </li>
    <li class="${!authImport ? 'disabled' : ''}">
        <g:link
                controller="menu"
                action="projectImport"
                params="[project: params.project ?: request.project]"
                title="${authImport ? '' : message(code: "request.error.unauthorized.title")}">
            <g:message code="import.archive.ellipsis" />
        </g:link>
    </li>
    <li role="separator" class="divider"></li>
    <li class="${!authDelete ? 'disabled' : ''}">
        <g:link
                controller="menu"
                action="projectDelete"
                params="[project: params.project ?: request.project]"
                title="${authDelete ? '' : message(code: "request.error.unauthorized.title")}">
            <g:icon name="remove"/>
            <g:message code="delete.project.ellipsis" />
        </g:link>
    </li>
</ul>

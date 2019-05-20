%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
        type: AuthConstants.TYPE_PROJECT,
        name: (params.project ?: request.project),
        context: "application"
)}"/>
<g:set var="authDelete"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_DELETE,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authExport"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_EXPORT,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authImport"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_IMPORT,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authConfigure"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_CONFIGURE,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: "application"
       )}"/>
<g:set var="authReadAcl"
       value="${auth.resourceAllowedTest(action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
                                         any: true,
                                         type: AuthConstants.TYPE_PROJECT_ACL,
                                         name: (params.project ?: request.project), context: "application"
       )}"/>

<div class="subnav" style="${wdgt.styleVisible(if:projConfigOpen)}">
  <ul class="nav" style="" data-old-padding-top="" data-old-padding-bottom="" data-old-overflow="">
    <li id="nav-project-settings-edit-project">
      <g:link
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProject"
            params="[project: params.project]">
        <span class="sidebar-mini">E</span> <span class="sidebar-normal"><g:message code="edit.configuration"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-edit-nodes">
      <g:link
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="projectNodeSources"
            params="[project: params.project]">
        <span class="sidebar-mini">N</span> <span class="sidebar-normal"><g:message code="edit.nodes"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-access-control">
      <g:link
            enabled="${authReadAcl}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectAcls"
            params="[project: params.project]"
      >
        <span class="sidebar-mini"><i class="fas fa-unlock-alt"></i></span> <span class="sidebar-normal"><g:message code="gui.menu.AccessControl"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-edit-readme">
      <g:link
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProjectFile"
            params="[project: params.project, filename: 'readme.md']"
      >
        <span class="sidebar-mini"><i class="far fa-file-alt"></i></span> <span class="sidebar-normal"><g:message code="edit.readme.ellipsis"/></span>
      </g:link>
    </li>

    <li id="nav-project-settings-edit-motd">
      <g:link
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProjectFile"
            params="[project: params.project, filename: 'motd.md']"
      >
        <span class="sidebar-mini"><i class="fas fa-comment-alt"></i></span> <span class="sidebar-normal"><g:message code="edit.message.of.the.day"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-setup-scm">
      <g:link
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="scm"
            action="index"
            params="[project: params.project]"
      >
        <span class="sidebar-mini"><i class="fas fa-exchange-alt"></i></span> <span class="sidebar-normal"><g:message code="project.admin.menu.Scm.title"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-export-archive">
      <g:link
            enabled="${authExport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectExport"
            params="[project: params.project]"
      >
        <span class="sidebar-mini"><i class="fas fa-download"></i></span> <span class="sidebar-normal"><g:message code="export.archive.ellipsis"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-import-archive">
      <g:link
            enabled="${authImport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectImport"
            params="[project: params.project]"
      >
        <span class="sidebar-mini"><i class="fas fa-upload"></i></span> <span class="sidebar-normal"><g:message code="import.archive.ellipsis"/></span>
      </g:link>
    </li>
    <li id="nav-project-settings-delete-project">
      <g:link
            enabled="${authExport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectDelete"
            params="[project: params.project]"
            icon="remove"
      >
        <span class="sidebar-mini"><i class="fas fa-trash"></i></span> <span class="sidebar-normal"><g:message code="delete.project.ellipsis"/></span>
      </g:link>
    </li>

      <g:ifMenuItems type="PROJECT_CONFIG"  project="${params.project}">
          <li role="separator" class="divider"></li>
            <g:forMenuItems type="PROJECT_CONFIG" var="item" project="${params.project}">
                <li>
                    <a href="${enc(attr:item.getProjectHref(params.project))}"
                       title="${enc(attr:g.message(code:item.titleCode,default:item.title))}">
                        <span class="sidebar-mini"><i class="${enc(attr: item.iconCSS ?: 'fas fa-plug')}"></i></span>
                        <span class="sidebar-normal">
                        <g:message code="${item.titleCode}" default="${item.title}"/>
                        </span>
                    </a>
                </li>
            </g:forMenuItems>
      </g:ifMenuItems>
  </ul>
</div>

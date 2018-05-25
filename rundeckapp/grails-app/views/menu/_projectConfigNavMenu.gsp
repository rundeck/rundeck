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

<bs:dropdown>

    <bs:menuitem headerCode="project.admin"/>

    <bs:menuitem
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProject"
            params="[project: params.project]"
            code="edit.configuration">
    </bs:menuitem>
    <bs:menuitem
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="projectNodeSources"
            params="[project: params.project]"
            code="edit.nodes">
    </bs:menuitem>

    <bs:menuitem
            enabled="${authReadAcl}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectAcls"
            params="[project: params.project]"
            code="gui.menu.AccessControl">
    </bs:menuitem>

    <bs:menuitem/>

    <bs:menuitem
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProjectFile"
            params="[project: params.project, filename: 'readme.md']"
            code="edit.readme.ellipsis">
    </bs:menuitem>

    <bs:menuitem
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="framework"
            action="editProjectFile"
            params="[project: params.project, filename: 'motd.md']"
            code="edit.message.of.the.day">
    </bs:menuitem>

    <bs:menuitem/>

    <bs:menuitem
            enabled="${authConfigure}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="scm"
            action="index"
            params="[project: params.project]"
            code="project.admin.menu.Scm.title">
    </bs:menuitem>

    <bs:menuitem/>

    <bs:menuitem
            enabled="${authExport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectExport"
            params="[project: params.project]"
            code="export.archive.ellipsis">
    </bs:menuitem>

    <bs:menuitem
            enabled="${authImport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectImport"
            params="[project: params.project]"
            code="import.archive.ellipsis">

    </bs:menuitem>

    <bs:menuitem/>

    <bs:menuitem
            enabled="${authExport}"
            disabledTitleCode="request.error.unauthorized.title"
            controller="menu"
            action="projectDelete"
            params="[project: params.project]"
            icon="remove"
            code="delete.project.ellipsis">
    </bs:menuitem>
</bs:dropdown>

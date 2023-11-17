%{--
  - Copyright 2015 SimplifyOps, Inc. (http://simplifyops.com)
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
   Author: Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
--%>
<%@ page import="org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="skipPrototypeJs" content="true"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code:filename=='readme.md'?'project.readme.title':filename=='motd.md'?'project.motd.title':'edit.project.file')}"/>
    <meta name="projconfigselected" content="${(filename=='readme.md'?'edit-readme':filename=='motd.md'?'edit-motd':'edit.project.file')}"/>
    <title><g:message code="edit.project.file" /></title>

    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="static/components/readme-motd.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    function init(){
        jQuery('input[type=text]').on('keydown', noenter);
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        jQuery('.apply_ace').each(function () {
            _setupAceTextareaEditor(this,confirm.setNeetsConfirm);
        });
    }
    jQuery(init);
    </g:javascript>
</head>

<body>
    <g:set var="authAdmin" value="${auth.resourceAllowedTest( action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN], type: AuthConstants.TYPE_PROJECT, name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION )}"/>
    <div class='vue-ui-socket'>
        <ui-socket section="edit-project-file" location="main"  :socket-data="{filename: '${filename}', displayConfig: '${displayConfig}', project: '${params.project ?: request.project}', authAdmin: '${authAdmin}'}"></ui-socket>
    </div>
</body>
</html>

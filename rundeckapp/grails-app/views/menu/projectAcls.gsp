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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 7/7/17
  Time: 9:01 AM
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'gui.menu.AccessControl')}"/>
    <title><g:message code="page.title.project.access.control.0" args="${[params.project]}"/></title>
</head>
<g:set var="hasEditAuth" value="${auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_ADMIN],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<g:set var="hasCreateAuth" value="${auth.resourceAllowedTest([
        any       : true,
        action    : [AuthConstants.ACTION_CREATE, AuthConstants.ACTION_ADMIN],
        context   : 'application',
        type      : AuthConstants.TYPE_PROJECT_ACL,
        attributes: [name: params.project]
]
)}"/>
<body>

<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>
<div class="row">

    <div class="col-sm-10 col-sm-offset-1">
        <div class="panel panel-default">
            <div class="panel-heading">
                <span class="panel-title">
                    <span class="text-info">${acllist.size()}</span>
                    <g:message code="project.access.control.prompt" args="${[params.project]}"/>
                </span>
            </div>

            <div class="panel-body">
                <div>
                    <g:if test="${hasCreateAuth}">
                        <div class="col-sm-12">
                            <g:link controller="menu"
                                    action="createProjectAclFile"
                                    params="${[project: params.project]}"
                                    class="btn btn-sm btn-success">
                                <g:icon name="plus"/>
                                <g:message code="access.control.action.create.acl.policy.button.title" />
                            </g:link>
                        </div>
                    </g:if>
                    <div class="grid">
                        <g:each in="${acllist}" var="file">
                            <g:render template="/menu/aclValidationTableRow"
                                      model="${[
                                              policyFile  : file,
                                              validation  : [valid: true],
                                              editHref    : hasEditAuth ? g.createLink(
                                                      [controller: 'menu', action: 'editProjectAclFile', params: [project: params.project, file: file]]
                                              ) : null,
                                              flashMessage: flash.storedFile == file ?
                                                      g.message(code: 'file.was.saved.flash.message.0',
                                                                args: [flash.storedSize]
                                                      ) : null
                            ]}"/>

                        </g:each>
                    </div>
                </div>

            </div>
        </div>
    </div>
</div>
</body>
</html>

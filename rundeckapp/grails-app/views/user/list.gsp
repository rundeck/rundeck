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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="user.list.title"/></title>

    %{-- Resolve view-level auth gates server-side so the Vue page does not re-implement them. --}%
    <g:set var="createAuthAllowed" value="${auth.resourceAllowedTest(
            kind: AuthConstants.TYPE_USER,
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
            any: true,
            context: AuthConstants.CTX_APPLICATION)}"/>
    <g:set var="editAuthAllowed" value="${auth.resourceAllowedTest(
            kind: AuthConstants.TYPE_USER,
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
            context: AuthConstants.CTX_APPLICATION)}"/>

    <feature:enabled name="vueUserList">
        %{-- Serialize ONLY safe display fields per user (never password/session/date fields). --}%
        <g:embedJSON id="userListData" data="${[
                users           : users.collect { [login: it.login, firstName: it.firstName, lastName: it.lastName, email: it.email] },
                createAuthAllowed: createAuthAllowed,
                editAuthAllowed  : editAuthAllowed
        ]}"/>
        <asset:javascript src="static/pages/user-list.js" defer="defer"/>
        <g:javascript>
            window._rundeck = Object.assign(window._rundeck || {}, {
                data: { }
            });
        </g:javascript>
    </feature:enabled>
</head>

<body>
<div class="content">
<div id="layoutBody">

<feature:disabled name="vueUserList">
<div class="row " id="userListPageLegacy">

        <div class="col-sm-10 col-sm-offset-1">
            <h3>Users

            <g:if test="${createAuthAllowed}">
                    <g:link action="create" class="btn btn-default btn-xs">
                        <i class="glyphicon glyphicon-plus"></i>
                        New Profile &hellip;
                    </g:link>
            </g:if></h3>
            <g:render template="/common/messages"/>


    <table cellpadding="0" cellspacing="0" width="100%" class="userlist">
        <g:each in="${users}" var="user" status="index">
            <tmpl:userListItem user="${user}" index="${index}"/>
        </g:each>
    </table>
    </div>

</div>
</feature:disabled>

<feature:enabled name="vueUserList">
<div class="row ">
    <div class="col-sm-10 col-sm-offset-1">
        <g:render template="/common/messages"/>
    </div>
    <div id="userListPage"></div>
</div>
</feature:enabled>

</div>
</div>
</body>
</html>

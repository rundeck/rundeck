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

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
e<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - User List</title>
</head>

<body>
<div class="row " id="userListPage">
        <div class="col-sm-3">
            <g:render template="/menu/configNav" model="[selected: 'profiles']"/>
        </div>

        <div class="col-sm-9">
            <h3>Users

            <g:if test="${auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.ACTION_ADMIN], context: 'application')}">
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
</body>
</html>



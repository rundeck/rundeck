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


<bs:dropdown>

    <bs:menuitem headerCode="system"/>
    <bs:menuitem
            controller="menu"
            action="storage"
            code="gui.menu.KeyStorage"/>
    <g:set var="authRead" value="${auth.resourceAllowedTest(
            type: 'resource',
            kind: 'system',
            action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
            any: true,
            context: 'application'
    )}"/>

    <g:if test="${authRead}">

        <bs:menuitem
                controller="menu"
                action="systemConfig"
                code="gui.menu.SystemConfig"/>
        <bs:menuitem
                controller="menu"
                action="acls"
                code="gui.menu.AccessControl"/>
        <bs:menuitem
                controller="menu"
                action="systemInfo"
                code="gui.menu.SystemInfo"/>


        <bs:menuitem
                shown="${g.logStorageEnabled()}"
                controller="menu"
                action="logStorage"
                code="gui.menu.LogStorage"/>

    </g:if>
    <bs:menuitem
            controller="menu"
            action="plugins"
            code="gui.menu.ListPlugins"/>

    <g:render template="/menu/sysConfigExecutionModeNavMenu"/>
</bs:dropdown>

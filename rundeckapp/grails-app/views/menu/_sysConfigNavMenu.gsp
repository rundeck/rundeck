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

<g:set var="authRead" value="${auth.resourceAllowedTest(
        type: 'resource',
        kind: 'system',
        action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
        any: true,
        context: 'application'
)}"/>

<ul class="dropdown-menu">
  <li class="dropdown-header">System</li>
  <li>
    <g:link controller="menu" action="storage">
        <g:message code="gui.menu.KeyStorage"/>
    </g:link>
  </li>
  <g:if test="${authRead}">
    <li>
      <g:link controller="menu" action="systemConfig">
        <g:message code="gui.menu.SystemConfig"/>
      </g:link>
    </li>
    <li>
      <g:link controller="menu" action="acls">
        <g:message code="gui.menu.AccessControl"/>
      </g:link>
    </li>
    <li>
      <g:link controller="menu" action="systemInfo">
        <g:message code="gui.menu.SystemInfo"/>
      </g:link>
    </li>
    <li>
      <g:link shown="${g.logStorageEnabled()}" controller="menu" action="logStorage">
        <g:message code="gui.menu.LogStorage"/>
      </g:link>
    </li>
  </g:if>
  <li>
    <g:link controller="menu" action="plugins">
      <g:message code="gui.menu.ListPlugins"/>
    </g:link>
  </li>
  <g:render template="/menu/sysConfigExecutionModeNavMenu"/>
</ul>

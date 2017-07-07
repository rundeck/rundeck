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

<ul class="dropdown-menu" role="menu" aria-labelledby="">

    <li class="dropdown-header">System</li>
    <g:if test="${auth.resourceAllowedTest(
            type: 'resource',
            kind: 'system',
            action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN],
            any: true,
            context: 'application'
    )
    }">
        <li class="${selected == 'storage' ? 'active' : ''}">
            <g:link controller="menu" action="storage">
                <g:message code="gui.menu.KeyStorage" default="Key Storage"/>
            </g:link>
        </li>
        <li class="${selected == 'syscfg' ? 'active' : ''}">
            <g:link controller="menu" action="systemConfig" >
                <g:message code="gui.menu.SystemConfig" default="System Configuration"/>
            </g:link>
        </li>
        <li class="">
            <g:link controller="menu" action="acls">
                <g:message code="gui.menu.AccessControl"/>
            </g:link>
        </li>
        <li class="">
            <g:link controller="menu" action="systemInfo" >
                <g:message code="gui.menu.SystemInfo" default="System Report"/>
            </g:link>
        </li>

        <g:if test="${g.logStorageEnabled() || selected == 'logstorage'}">

            <li class="${selected == 'logstorage' ? 'active' : ''}">
                <g:link controller="menu" action="logStorage" >
                    <g:message code="gui.menu.LogStorage" default="Log Storage"/>
                </g:link>
            </li>

        </g:if>

    </g:if>
    <li class="${selected == 'plugins' ? 'active' : ''}">
        <g:link controller="menu" action="plugins" >
            <g:message code="gui.menu.ListPlugins"/>
        </g:link>
    </li>

    <g:render template="/menu/sysConfigExecutionModeNavMenu"/>
</ul>
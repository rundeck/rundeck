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

<g:set var="authAction" value="${g.executionMode(active: true) ? AuthConstants.ACTION_DISABLE_EXECUTIONS :
        AuthConstants.ACTION_ENABLE_EXECUTIONS}"/>
<auth:resourceAllowed action="${[authAction, AuthConstants.ACTION_ADMIN]}"
                      any="true"
                      context="application"
                      kind="system">
    <bs:menuitem/>
    <bs:menuitem
            action="executionMode"
            controller="menu">
        <g:ifExecutionMode>
            <g:icon name="play" css="text-success"/>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <g:icon name="pause" css="text-warning"/>
        </g:ifExecutionMode>
        <g:message code="execution.mode.ellipsis"/>
    </bs:menuitem>
</auth:resourceAllowed>

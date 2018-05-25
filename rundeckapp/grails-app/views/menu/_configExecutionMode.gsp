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
    <div class="form-group">
        <label class=" control-label"><g:message code="executionMode.label"/>:</label>

        <g:ifExecutionMode>
            <p class=" form-control-static text-success"><g:message code="system.executionMode.status.active"/></p>

            <p class="help-block text-success">
                <g:message code="system.executionMode.description.active"/>
            </p>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <p class=" form-control-static text-warning"><g:message code="system.executionMode.status.passive"/></p>

            <p class="help-block text-warning">
                <g:message code="system.executionMode.description.passive"/>
            </p>
        </g:ifExecutionMode>
    </div>
<g:if test="${selected != 'changeexecmode'}">

    <g:set var="authAction" value="${g.executionMode(active:true)?AuthConstants.ACTION_DISABLE_EXECUTIONS:AuthConstants.ACTION_ENABLE_EXECUTIONS}"/>
    <auth:resourceAllowed action="${[authAction,AuthConstants.ACTION_ADMIN]}" any="true" context="application" kind="system">
        <g:ifExecutionMode active="true">
            <g:link action="executionMode" controller="menu" class="btn btn-default btn-sm">
                <g:message code="change.execution.mode" />
            </g:link>

        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <g:link action="executionMode" controller="menu" class="btn btn-default btn-sm">
                <g:message code="change.execution.mode" />
            </g:link>
        </g:ifExecutionMode>
    </auth:resourceAllowed>

</g:if>
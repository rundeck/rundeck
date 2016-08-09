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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 7/13/15
  Time: 11:50 AM
--%>

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title><g:message code="gui.menu.ExecutionMode" default="Execution Mode" /></title>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>

</head>

<body>
<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'changeexecmode']"/>
    </div>
    <div class="col-sm-9">

        <g:form class="form-horizontal" controller="execution" action="executionMode" method="POST" useToken="true">
        <div class="panel panel-primary">
            <div class="panel-heading">
                <h3 class="panel-title"><g:message code="change.execution.mode" /></h3>
            </div>
            <div class="panel-body">


            <g:hiddenField name="project" value="${params.project}"/>
            <div class="form-group">
                <div class="radio  col-sm-12 ">
                <label class="control-label text-success">
                    <g:radio name="mode" value="active" checked="${g.executionMode(active: true)}"/>
                    <g:message code="system.executionMode.status.active"/>
                    <g:ifExecutionMode active="true">(Current Mode)</g:ifExecutionMode>
                </label>

                    <p class="col-sm-12 help-block ">
                        <g:message code="system.executionMode.description.active"/>
                    </p>
                </div>

                <div class="radio  col-sm-12 ">
                <label class="control-label text-warning">
                    <g:radio name="mode" value="passive" checked="${g.executionMode(active: false)}"/>
                    <g:message code="system.executionMode.status.passive"/>
                    <g:ifExecutionMode passive="true">(Current Mode)</g:ifExecutionMode>
                </label>

                    <p class="col-sm-12 help-block ">
                        <g:message code="system.executionMode.description.passive"/>
                    </p>
                </div>

                <g:ifExecutionMode>


                    <p class="col-sm-12 help-block text-info">
                        <g:message code="action.executionMode.set.passive.help"/>
                    </p>
                </g:ifExecutionMode>
                <g:ifExecutionMode passive="true">


                    <p class="col-sm-12 help-block text-info">
                        <g:message code="action.executionMode.set.active.help"/>
                    </p>
                </g:ifExecutionMode>
            </div>

            </div>
            <div class="panel-footer">
                <div class="form-group ">
                <div class="col-sm-12 ">
                <g:link action="systemConfig" controller="menu" class="btn btn-default " name="cancel">
                    <g:message code="cancel"/>
                </g:link>
            <g:set var="authAction" value="${g.executionMode(active:true)?AuthConstants.ACTION_DISABLE_EXECUTIONS:AuthConstants.ACTION_ENABLE_EXECUTIONS}"/>
            <auth:resourceAllowed action="${[authAction,AuthConstants.ACTION_ADMIN]}" any="true" context="application" kind="system">
                    <button type="submit"
                            class="btn btn-primary "
                            >
                        Set Execution Mode
                    </button>
            </auth:resourceAllowed>
                </div>
                </div>
            </div>
        </div>
        </g:form>

</div>
</div>

</body>
</html>
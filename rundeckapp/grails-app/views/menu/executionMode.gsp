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

<%@ page import="org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title><g:message code="gui.menu.ExecutionMode" default="Execution Mode" /></title>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${g.message(code:'gui.menu.ExecutionMode')}"/>

</head>

<body>
<div class="content">
<div id="layoutBody">
  <div class="title">
    <span class="text-h3"><i class="fas fa-play"></i> ${g.message(code:"gui.menu.ExecutionMode")}</span>
  </div>
  <div class="container-fluid">
    <div class="row">
        <div class="col-sm-12">
            <g:render template="/common/messages"/>
        </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <g:form class="form-horizontal" controller="execution" action="executionMode" method="POST" useToken="true">
          <div class="card-header">
            <h4 class="card-title">
              <g:message code="change.execution.mode" />
            </h4>
          </div>
          <div class="card-content">
            <g:hiddenField name="project" value="${params.project}"/>
            <div>
              <div class="radio">
                <g:radio name="mode" value="active" id="activemode" checked="${g.executionMode(active: true)}"/>
                <label for="activemode">
                  <g:icon name="play" />
                  <g:message code="system.executionMode.status.active"/>
                  <g:ifExecutionMode active="true"><g:message code="current.mode" /></g:ifExecutionMode>
                </label>
                <p class="help-block ">
                  <g:message code="system.executionMode.description.active"/>
                </p>
              </div>
              <div class="radio">
                <g:radio name="mode" value="passive" id="passivemode" checked="${g.executionMode(active: false)}"/>
                <label for="passivemode">
                  <g:icon name="pause" />
                  <g:message code="system.executionMode.status.passive"/>
                  <g:ifExecutionMode passive="true"><g:message code="current.mode" /></g:ifExecutionMode>
                </label>
                <p class="help-block ">
                  <g:message code="system.executionMode.description.passive"/>
                </p>
              </div>
              <g:ifExecutionMode>
              <div class="col-xs-12">
                <div class="alert alert-info">
                  <span><g:message code="action.executionMode.set.passive.help"/></span>
                </div>
              </div>
              </g:ifExecutionMode>
              <g:ifExecutionMode passive="true">
                <div class="col-xs-12">
                  <div class="alert alert-info">
                    <span><g:message code="action.executionMode.set.active.help"/></span>
                  </div>
                </div>
              </g:ifExecutionMode>
            </div>
          </div>
          <div class="card-footer">
            <hr style="display:block;width:100%;">
            <g:link action="index" controller="menu" class="btn btn-default " name="cancel">
              <g:message code="cancel"/>
            </g:link>
            <g:set var="authAction" value="${g.executionMode(active: true) ? AuthConstants.ACTION_DISABLE_EXECUTIONS : AuthConstants.ACTION_ENABLE_EXECUTIONS}"/>
            <auth:resourceAllowed action="${[authAction, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN, AuthConstants.ACTION_APP_ADMIN]}" any="true" context="${AuthConstants.CTX_APPLICATION}" kind="${AuthConstants.TYPE_SYSTEM}">
              <button type="submit" class="btn btn-cta">
                <g:message code="set.execution.mode"/>
              </button>
            </auth:resourceAllowed>
          </div>
          </g:form>
        </div>
      </div>
    </div>
  </div>
</div>
</div>
</body>
</html>

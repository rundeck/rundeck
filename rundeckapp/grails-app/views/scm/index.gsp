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
  Date: 4/30/15
  Time: 3:27 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'gui.menu.Scm')}"/>
    <meta name="projconfigselected" content="setup-scm"/>
    <meta name="layout" content="base"/>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[params.project]:params.project}"/>
    <title><g:appTitle/> - <g:message code="scmController.page.index.title" args="[projectLabel]"/></></title>

</head>

<body>
  <div class="container-fluid">
    <div class="row">
      <div class="col-sm-12">
        <g:render template="/common/messages"/>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div class="card card-default">
          <div class="card-header">
            <h3 class="card-title">
              <g:message code="gui.menu.Scm" default="Setup SCM"/>
            </h3>
          </div>
          <div class="card-content">
            <p class="text-info">
              <g:message code="scmController.page.index.description" default="Enable or configure SCM integration."/>
            </p>
            <div class="list-group">
              <g:each in="['export', 'import']" var="integration">
                <div class="list-group-item">
                  <div class="list-group-item-heading">
                    <h4 style="margin:5px 0 10px">
                      <g:message code="scm.${integration}.title"/>
                    </h4>
                  </div>
                  <g:render template="pluginConfigList" model="[
                    integration     : integration,
                    pluginConfig    : pluginConfig[integration],
                    enabled         : enabled[integration],
                    configuredPlugin: configuredPlugin[integration],
                    plugins         : plugins[integration]
                  ]"/>
                </div>
              </g:each>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>

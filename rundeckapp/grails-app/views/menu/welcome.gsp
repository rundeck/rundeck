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

<%--
Created by IntelliJ IDEA.
User: greg
Date: 4/18/16
Time: 12:54 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="base"/>
  <meta name="tabpage" content="home"/>
  <title><g:appTitle/><g:message code="page.welcome.title.suffix"/></title>
  <asset:javascript src="static/pages/login.js"/>
  <style media="screen">
    .card-footer .table{
      margin-bottom: 0;
    }
  </style>
</head>
<body>
<div class="content">
<div id="layoutBody">
  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-header">
            <div class="vue-ui-socket">
              <ui-socket section="menu-welcome"
                         location="menu-welcome-header"
                         socket-data="${g.enc(attr:[appTitle: g.appTitle()].encodeAsJSON())}">
                <h2 class="card-title">
                  <g:message code="app.firstRun.title"
                             args="${[g.appTitle(), grailsApplication.metadata['build.ident']]}"/>
                </h2>
              </ui-socket>
            </div>
          </div>
          <div class="card-content">
            <div class="ui-common-platform enterprise-hide" style="margin:0 0 2em;">
              <h3 >UNSUPPORTED SOFTWARE. NO WARRANTY.</h3>
            </div>
            <g:set var="logoImage" value="${"static/img/${g.appLogo()}"}"/>
            <asset:image src="${logoImage}" alt="${[g.appTitle()]}" style="width: 400px; padding-bottom: 10px" onload="SVGInject(this)"/>
            <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>
            <div style="margin-top:2em;">
              <g:link controller="menu" action="index" class="btn btn-lg btn-primary">
              <g:message code="welcome.button.use.rundeck" args="${[g.appTitle()]}"/>
            </g:link>
          </div>
        </div>
        <div class="card-footer" style="margin-top:1em;">
            <div class="vue-ui-socket">
              <ui-socket section="menu-welcome-footer"
                         location="menu-welcome-footer-build-info"
                >
                <g:basicData data="${buildData}" fields="${buildDataKeys.sort()}"/>
                <g:render template="/common/versionDisplay"/>
              </ui-socket>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</div>
</body>
</html>

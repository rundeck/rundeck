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
  Date: 10/4/13
  Time: 10:23 AM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="grails.converters.JSON; org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="base"/>
  <meta name="tabpage" content="home"/>
  <meta name="skipPrototypeJs" content="true"/>

  <title><g:appTitle/></title>
  <g:set var="uiType" value="${params.nextUi ? 'next' : params.legacyUi ? 'legacy' : 'current'}"/>
  <g:if test="${!projectNames}">
    <g:embedJSON data="${[projectNames: [], projectNamesTotal: -1]}" id="projectNamesData"/>
  </g:if>
  <g:elseif test="${projectNames && projectNames.size() < 50}">
    <g:embedJSON data="${[projectNames: projectNames, projectNamesTotal: projectNames.size()]}"
                 id="projectNamesData"/>
  </g:elseif>
  <g:elseif test="${projectNames}">
    <g:embedJSON data="${[projectNames: projectNames[0..49], projectNamesTotal: projectNames.size()]}"
                 id="projectNamesData"/>
  </g:elseif>
  <g:embedJSON
          data="${[loaded: statsLoaded, execCount: execCount, totalFailedCount: totalFailedCount, recentUsers: recentUsers, recentProjects: recentProjects]}"
          id="statsData"/>

  <g:embedJSON data="${[
          detailBatchMax  : params.getInt('detailBatchMax') ?: cfg.getInteger(config: 'gui.home.projectList.detailBatchMax', default: 15),
          summaryRefresh  : cfg.getBoolean(config: 'gui.home.projectList.summaryRefresh', default: true),
          refreshDelay    : cfg.getInteger(config: 'gui.home.projectList.summaryRefreshDelay', default: 30000),
          detailBatchDelay: params.getInt('detailBatchDelay') ?: cfg.getInteger(config: 'gui.home.projectList.detailBatchDelay', default: 1000).toInteger(),
          pagingEnabled   : params.getBoolean('pagingEnabled', cfg.getBoolean(config: 'gui.home.projectList.pagingEnabled', default: true)),
          pagingMax       : params.getInt('pagingMax') ?: cfg.getInteger(config: 'gui.home.projectList.pagingMax', default: 30),
  ]}" id="homeDataPagingParams"/>

  <!-- VUE JS REQUIREMENTS -->
  <asset:javascript src="static/pages/home.js" defer="defer"/>
  <asset:javascript src="static/components/ko-paginator.js"/>
  <!-- /VUE JS REQUIREMENTS -->

  <asset:javascript src="menu/home.js"/>

  <!-- VUE CSS MODULES -->
  <asset:stylesheet href="static/css/components/version-notification.css"/>
  <!-- /VUE CSS MODULES -->
  <asset:javascript src="static/pages/login.js"/>
  <style type="text/css">
  .project_list_item_link {
    display: inline-block;
    width: calc(100% - 20px);
    padding: 8px 0;
  }
  </style>
</head>

<body>
<div class="content">
  <div id="layoutBody">
    <div class="vue-ui-socket">
      <g:set var="createProjectAllowed"
             value="${auth.resourceAllowedTest(action: AuthConstants.ACTION_CREATE, type: AuthConstants.TYPE_PROJECT, context: AuthConstants.CTX_APPLICATION)}"/>
      <g:set var="roles"
             value="${request.subject?.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)?.collect { it.name }}"/>
      <g:set var="buildIdent" value="${servletContextAttribute(attribute: 'app.ident')}"/>
      <g:set var="appTitle" value="${g.appTitle()}"/>
      <g:set var="logoImage" value="${"static/img/${g.appLogo()}"}"/>
      <g:set var="helpLinkUrl" value="${g.helpLinkUrl()}"/>

      <ui-socket section="home" location="list" :socket-data="{
                createProjectAllowed: ${createProjectAllowed},
                roles: ${enc(attr: roles.encodeAsJSON())},
                isFirstRun: ${isFirstRun},
                appTitle: '${appTitle}',
                buildIdent: '${buildIdent}',
                logoImage: '${logoImage}',
                helpLinkUrl: '${helpLinkUrl}',
                }"></ui-socket>
    </div>
  </div>
  <!-- VUE JS MODULES -->
  <asset:stylesheet href="static/css/pages/home.css"/>
  <asset:stylesheet href="static/css/components/first-run.css"/>
  <asset:javascript src="static/components/first-run.js"/>
  <asset:javascript src="static/components/version-notification.js"/>
  <!-- /VUE JS MODULES -->
</body>
</html>

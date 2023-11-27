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

<%@ page import="grails.util.Environment; org.rundeck.core.auth.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <g:set var="projectName" value="${params.project ?: request.project}"></g:set>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <g:set var="filtvalue" value="${query?.('filter')}"/>
    <title><g:message code="gui.menu.Nodes"/> - <g:enc>${projectLabel}</g:enc></title>
    <asset:javascript src="framework/nodes.js"/>
    <asset:javascript src="static/pages/nodes.js" defer="defer"/>
    <asset:stylesheet src="static/css/pages/nodes.css"/>
    <g:embedJSON id="filterParamsJSON"
                 data="${[filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
    <g:embedJSON id="pageParams"
                 data="${[pagingMax:params.int('max')?:20, project:params.project?:request.project]}"/>

    <g:jsMessages code="Node,Node.plural"/>
    <g:set var="legacyUi" value="${params.legacyUi || feature.isEnabled(name:'legacyUi')}"/>
</head>
<body>


<g:set var="run_authorized" value="${auth.adhocAllowedTest( action:AuthConstants.ACTION_RUN,project: params.project ?: request.project)}"/>
<g:set var="job_create_authorized" value="${auth.resourceAllowedTest(kind:AuthConstants.TYPE_JOB, action: AuthConstants.ACTION_CREATE,project: params.project ?: request.project)}"/>
<div class="content">
    <div id="layoutBody">
        <g:if test="${legacyUi}">
            <tmpl:legacyNodesList />
        </g:if>
        <g:else>
            <div class="vue-ui-socket">
                <ui-socket section="nodes-page" location="main" socket-data="${enc(attr: [filter: filtvalue?:'', showInputTitle: true, autofocus: false, runAuthorized: run_authorized, jobCreateAuthorized: job_create_authorized].encodeAsJSON())}"></ui-socket>
            </div>
        </g:else>

    </div>
</div>
</body>
</html>
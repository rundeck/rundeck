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
    <meta name="tabpage" content="projectHome"/>
    <title><g:appTitle/> - ${session.frameworkLabels?session.frameworkLabels[project]:project}</title>
    <g:embedJSON data="${[project: project]}" id="projectData"/>
    <asset:stylesheet href="static/css/pages/project-dashboard.css"/>
    <g:jsMessages code="jobslist.date.format.ko,select.all,select.none,delete.selected.executions,cancel.bulk.delete,cancel,close,all"/>
    <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
                context: 'application', type: 'project', name: params.project, action: AuthConstants.ACTION_ADMIN)}"/>
        <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: 'application', type: 'project', name:
                params.project, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>
        <g:set var="projectEventsAuth" value="${auth.resourceAllowedTest(kind: 'event', project: params.project, action: AuthConstants.ACTION_READ) || projAdminAuth}"/>
    <g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data:{
            projectEventsAuth:${enc(js:projectEventsAuth)},
            projectAdminAuth:${enc(js:projAdminAuth)},
            deleteExecAuth:${enc(js:deleteExecAuth)},
            jobslistDateFormatMoment:"${enc(js:g.message(code:'jobslist.date.format.ko'))}",
            runningDateFormatMoment:"${enc(js:g.message(code:'jobslist.running.format.ko'))}",
            activityUrl: appLinks.reportsEventsAjax,
            bulkDeleteUrl: appLinks.apiExecutionsBulkDelete,
            activityPageHref:"${enc(js:createLink(controller:'reports',action:'index',params:[project:params.project]))}"
        }
    })
    </g:javascript>
    <style type="text/css">

    .subtitlebar.no-min-height {
        min-height: auto;
    }
    .text-project-description{
        margin: 5px 0;
        display: block;
    }
    </style>
    <asset:javascript src="menu/projectHome.js"/>
</head>

<body>
<content tag="subtitlecss">plain no-min-height</content>
<content tag="subtitlesection">

<div class="container-fluid" >
<div class="row">
<div class="col-xs-12">
<div data-ko-bind="projectHome">
  <h2> <span data-bind="text: project().label"></span></h2>
  <span class="text-h4 text-project-description" >
      <span data-bind="text: project().description"></span>
  </span>
</div>
</div>
</div>

</div>
</content>
<div class="content">
<div id="layoutBody">
  <div class="conntainer-fluid">
    <div class="row">
        <div class="col-xs-12">
            <g:render template="/common/messages"/>
        </div>
    </div>

    <div id="projectHome-summary" class="project-dashboard-vue">
      <App :event-bus="EventBus" show-description="false" show-readme="false"/>
    </div>

    <div id="projectHome-content" class="project-dashboard-vue">
      <App :event-bus="EventBus" show-description="false" show-summary="false"/>
    </div>

  </div>
  <asset:javascript src="static/pages/project-dashboard.js"/>
</div>
</div>
</body>
</html>

%{--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
  Date: 2019-05-15
  Time: 05:08
--%>

<%@ page import="org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="events"/>
    <meta name="skipPrototypeJs" content="true"/>
    <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
        <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${createLink(controller:"feed",action:"index",params:paginateParams?paginateParams:[:])}"/>
    </g:ifServletContextAttribute>
    <g:set var="projectName" value="${params.project ?: request.project}"/>

    <title><g:message code="gui.menu.Events"/> - <g:enc>${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}</g:enc></title>

    <g:set var="pageparams" value="${[offset:params.offset,max:params.max]}"/>
    <g:set var="eventsparams" value="${params.subMap([
            'titleFilter',
            'jobFilter',
            'jobIdFilter',
            'userFilter',
            'statFilter',
            'filter',
            'recentFilter',
            'startbeforeFilter',
            'startafterFilter',
            'endbeforeFilter',
            'endafterFilter',
            'filterName'
    ])}"/>
    <g:embedJSON id="eventsparamsJSON" data="${eventsparams}"/>
    <g:embedJSON id="pageparamsJSON" data="${pageparams}"/>
    <asset:stylesheet href="static/css/pages/project-dashboard.css"/>
    <g:jsMessages code="jobslist.date.format.ko,select.all,select.none,delete.selected.executions,cancel.bulk.delete,cancel,close,all,bulk.delete,running"/>
    <g:jsMessages code="search.ellipsis
jobquery.title.titleFilter
jobquery.title.jobFilter
jobquery.title.jobIdFilter
jobquery.title.userFilter
jobquery.title.statFilter
jobquery.title.filter
jobquery.title.recentFilter
jobquery.title.startbeforeFilter
jobquery.title.startafterFilter
jobquery.title.endbeforeFilter
jobquery.title.endafterFilter
saved.filters
search
"/>
    <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
            context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: projectName, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])}"/>
    <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name:
            projectName, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>
    <cfg:setVar var="defaultMax" defaultValue="${30}" key="pagination.default.max"/>
    <g:set var="pageMax" value="${params.max?params.int('max',defaultMax):defaultMax}"/>
    <g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data:{
            projectAdminAuth:${enc(js:projAdminAuth)},
            deleteExecAuth:${enc(js:deleteExecAuth)},
            jobslistDateFormatMoment:"${enc(js:g.message(code:'jobslist.date.format.ko'))}",
            runningDateFormatMoment:"${enc(js:g.message(code:'jobslist.running.format.ko'))}",
            activityUrl: appLinks.reportsEventsAjax,
            nowrunningUrl: "${createLink(uri:"/api/${com.dtolabs.rundeck.app.api.ApiVersions.API_CURRENT_VERSION}/project/${projectName}/executions/running")}",
            bulkDeleteUrl: appLinks.apiExecutionsBulkDelete,
            activityPageHref:"${enc(js:createLink(controller:'reports',action:'index',params:[project:projectName]))}",
            sinceUpdatedUrl:"${enc(js:g.createLink(controller:'reports',action: 'since.json', params: [project:projectName]))}",
            pagination:{
                max: ${enc(js:pageMax)}
            },
            query:loadJsonData('eventsparamsJSON')
        }
    })
    </g:javascript>
    <asset:javascript src="static/pages/project-activity.js" defer="defer"/>
</head>
<body>
<div class="content">
<div id="layoutBody">
  <div class="title">
    <span class="text-h3"><i class="fas fa-history"></i> ${g.message(code:"gui.menu.Events")}</span>
  </div>
    <div class="pageBody container-fluid">
        <g:render template="/common/messages"/>

        <div class="row vue-project-activity">
            <div class="col-xs-12">
                <div class="card">
                    <div class="card-content">
                        <activity-list :event-bus="EventBus"></activity-list>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
</div>
</body>
</html>

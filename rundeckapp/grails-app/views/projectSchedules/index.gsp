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
  User: carlos
  Date: 2019-10-05
  Time: 20:22
--%>

<%@ page import="com.dtolabs.rundeck.core.authorization.AuthContext" contentType="text/html;charset=UTF-8" %>
<html>
<head>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="schedules"/>
    <meta name="skipPrototypeJs" content="true"/>
    <g:set var="projectName" value="${params.project ?: request.project}"/>

    <title><g:message code="gui.menu.Schedules"/> - <g:enc>${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}</g:enc></title>

    <g:javascript>
    window._rundeck = Object.assign(window._rundeck || {}, {
        data:{
            //projectAdminAuth:${enc(js:projAdminAuth)},
            //deleteExecAuth:${enc(js:deleteExecAuth)},
            jobslistDateFormatMoment:"${enc(js:g.message(code:'jobslist.date.format.ko'))}",
            runningDateFormatMoment:"${enc(js:g.message(code:'jobslist.running.format.ko'))}",
            activityUrl: appLinks.reportsEventsAjax,
            nowrunningUrl: "${createLink(uri:"/api/${com.dtolabs.rundeck.app.api.ApiVersions.API_CURRENT_VERSION}/project/${projectName}/executions/running")}",
            bulkDeleteUrl: appLinks.apiExecutionsBulkDelete,
            activityPageHref:"${enc(js:createLink(controller:'reports',action:'index',params:[project:projectName]))}",
            sinceUpdatedUrl:"${enc(js:g.createLink(controller:'reports',action: 'since.json', params: [project:projectName]))}",
            filterListUrl:"${enc(js:g.createLink(controller:'reports',action: 'listFiltersAjax', params: [project:projectName]))}",
            filterSaveUrl:"${enc(js:g.createLink(controller:'reports',action: 'saveFilterAjax', params: [project:projectName]))}",
            filterDeleteUrl:"${enc(js:g.createLink(controller:'reports',action: 'deleteFilterAjax', params: [project:projectName]))}",
            pagination:{
                max: ${enc(js:params.max?params.int('max',30):30)}
        }
    }
})
    </g:javascript>

    <asset:stylesheet href="static/css/pages/project-schedule.css"/>
    <asset:javascript src="static/pages/project-schedule.js" defer="defer"/>
</head>
<body>

<div>
    <div class="pageBody container-fluid">
        <g:render template="/common/messages"/>

        <div class="row">
            <div class="col-sm-12">
                <div id="vue-project-schedules"></div>
            </div>
        </div>

    </div>
</div>
</body>
</html>

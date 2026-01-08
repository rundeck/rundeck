%{--
  Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<%@ page import="grails.util.Environment; org.rundeck.core.auth.AuthConstants" %>
<html>
<head>
    <g:set var="ukey" value="${g.rkey()}" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="adhoc"/>
  <g:set var="projectName" value="${params.project ?: request.project}"/>
    <g:set var="projectLabel" value="${session.frameworkLabels?session.frameworkLabels[projectName]:projectName}"/>
    <title><g:message code="gui.menu.Adhoc"/> - <g:enc>${projectLabel}</g:enc></title>

  <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
          context: AuthConstants.CTX_APPLICATION,
          type: AuthConstants.TYPE_PROJECT,
          name: projectName,
          action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
          any: true
  )}"/>
  <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(
          context: AuthConstants.CTX_APPLICATION,
          type: AuthConstants.TYPE_PROJECT,
          name: projectName,
          action: AuthConstants.ACTION_DELETE_EXECUTION
  ) || projAdminAuth}"/>

  <g:set var="eventReadAuth" value="${auth.resourceAllowedTest(
          project: projectName,
          action: AuthConstants.ACTION_READ,
          kind: AuthConstants.TYPE_EVENT
  )}"/>

    <asset:stylesheet src="static/css/pages/command.css"/>
    <asset:javascript src="executionState.js"/>
    <asset:javascript src="executionControl.js"/>
    <asset:javascript src="util/yellowfade.js"/>
    <asset:javascript src="static/pages/nodes.js" defer="defer"/>
    <asset:stylesheet src="static/css/pages/nodes.css"/>
    <g:set var="defaultLastLines" value="${cfg.getInteger(config: "gui.execution.tail.lines.default", default: 20)}"/>
    <g:set var="maxLastLines" value="${cfg.getInteger(config: "gui.execution.tail.lines.max", default: 20)}"/>

    <g:embedJSON id="filterParamsJSON" data="${[matchedNodesMaxCount: matchedNodesMaxCount?:50, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
    <g:embedJSON id="pageParams" data="${[
            disableMarkdown: params.boolean('disableMarkdown') ? '&disableMarkdown=true' :'',
            smallIconUrl:resource(dir: 'images', file: 'icon-small'),
            iconUrl:resource(dir: 'images', file: 'icon-small'),
            lastlines:params.int('lastlines')?: defaultLastLines,
            maxLastLines:params.int('maxlines')?: maxLastLines,
            emptyQuery:emptyQuery?:null,
            ukey:ukey,
            project:params.project?:request.project,
            runCommand:runCommand?:'',
            adhocKillAllowed:auth.adhocAllowedTest(action: AuthConstants.ACTION_KILL,project:params.project)
    ]}"/>
    <g:jsMessages code="Node,Node.plural"/>

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

  <g:set var="executionModeActive" value="${g.executionMode(active: true, project: projectName)}"/>
  <g:set var="uiType" value="${params.nextUi?'next':params.legacyUi?'legacy':'current'}"/>
  <g:set var="nextUi" value="${uiType == 'next'}"/>
  <g:set var="legacyUi" value="${uiType == 'legacy' || (!nextUi && !params.nextUi)}"/>

  <g:javascript>

    window._rundeck = Object.assign(window._rundeck || {}, {
        data:{
            projectAdminAuth:${enc(js:projAdminAuth)},
            deleteExecAuth:${enc(js:deleteExecAuth)},
            eventReadAuth:${enc(js:eventReadAuth)},
            executionModeActive:${enc(js:executionModeActive)},
            jobslistDateFormatMoment:"${enc(js:g.message(code:'jobslist.date.format.ko'))}",
            runningDateFormatMoment:"${enc(js:g.message(code:'jobslist.running.format.ko'))}",
            activityUrl: appLinks.reportsEventsAjax,
            bulkDeleteUrl: appLinks.apiExecutionsBulkDelete,
            activityPageHref:"${enc(js:createLink(controller:'reports',action:'index',params:[project:projectName]))}",
            sinceUpdatedUrl:"${enc(js:g.createLink(controller:'reports',action: 'since.json', params: [project:projectName]))}",
            pagination:{
                max: ${enc(js:params.max?params.int('max',10):10)}
    },
    query:{
        jobIdFilter:'null'
      },
      filterOpts: {
          showFilter: false,
          showRecentFilter: true,
          showSavedFilter: false
      },
      runningOpts: {
          loadRunning:false,
          allowAutoRefresh: false
      }
}
})
  </g:javascript>
  <asset:javascript src="static/pages/project-activity.js" defer="defer"/>
  <g:if test="${nextUi}">
    <asset:javascript src="static/pages/adhoc.js" defer="defer"/>
  </g:if>
  <g:else>
    <asset:javascript src="static/pages/command.js" defer="defer"/>
  </g:else>
</head>
<body>

<div class="content">
<div id="layoutBody">
  <div class="title">
    <span class="text-h3"><i class="fas fa-terminal"></i> ${g.message(code:"gui.menu.Adhoc")}</span>
  </div>
  
  <!-- Node Filter Input (via ui-socket - already Vue) -->
<div>
    <div>
      <div class="row">
        <div class="col-xs-12">
          <div class="${emptyQuery ? 'active' : ''}">
            <g:form action="adhoc" class="form form-horizontal" name="searchForm">
              <g:hiddenField name="max" value="${max}"/>
              <g:hiddenField name="offset" value="${offset}"/>
              <g:hiddenField name="formInput" value="true"/>
              <g:set var="filtvalue" value="${query?.('filter')}"/>
              <div class="form-group">
                <div class="col-sm-12 vue-ui-socket">
                  <ui-socket section="adhoc-command-page"
                             location="node-filter-input"
                             socket-data="${enc(attr: [filter: filtvalue?:'', showInputTitle: true, autofocus: !filtvalue].encodeAsJSON())}">
                  </ui-socket>
                </div>
              </div>
            </g:form>
          </div>
        </div>
      </div>
    </div>
</div>

  <!-- CSRF Token for form submissions -->
  <g:jsonToken id="adhoc_req_tokens" url="${request.forwardURI}"/>

  <!-- Messages and Error Areas -->
<div class="container-fluid page-commands">
  <div id="nodesContent" class="row">
    <g:render template="/common/messages"/>
      <g:if test="${nextUi}">
        <!-- Vue App Mount Point -->
        <div class="adhoc-page-vue"></div>
      </g:if>
      <g:else>
        <g:render template="/framework/legacyadhoc" model="[projectName: projectName, eventReadAuth: eventReadAuth]"/>
      </g:else>
    </div>
    <div id="loaderror"></div>
            </div>

  <g:if test="${eventReadAuth}">
    <div id="activity_section" class="vue-ui-socket">
      <ui-socket section="project-activity" location="main"></ui-socket>
    </div>
  </g:if>
</div>
</div>
</body>
</html>

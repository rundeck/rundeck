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

<%@ page import="org.rundeck.core.auth.AuthConstants; grails.util.Environment; rundeck.Execution; rundeck.ScheduledExecution" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="events"/>
    <meta name="layout" content="base" />
    <meta name="skipPrototypeJs" content="base" />

    <title><g:appTitle/> -
    %{--      <g:if test="${null==execution?.dateCompleted}"><g:message code="now.running" /> - </g:if>--}%
      <g:if test="${scheduledExecution}"><g:enc>${scheduledExecution?.jobName}</g:enc> :  </g:if>
      <g:else><g:message code="execution.type.adhoc.title" /></g:else> <g:message code="execution.at.time.by.user" args="[g.relativeDateString(atDate:execution.dateStarted),execution.user]"/>
    </title>
      <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:'tail'}"/>
      <g:set var="authKeys" value="${[AuthConstants.ACTION_KILL,
                                      AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW, AuthConstants.ACTION_CREATE, AuthConstants.ACTION_RUN]}"/>
      <g:set var="authChecks" value="${[:]}"/>
      <g:each in="${authKeys}" var="actionName">
      <g:if test="${execution.scheduledExecution}">
          <%-- set auth values --%>
          %{
              authChecks[actionName]=auth.jobAllowedTest(job:execution.scheduledExecution,action: actionName)
          }%
      </g:if>
      <g:else>
          %{
              authChecks[actionName] = auth.adhocAllowedTest(action: actionName,project:execution.project)
          }%
      </g:else>
      </g:each>
      <g:set var="adhocRunAllowed" value="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN,project:execution.project)}"/>
      <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: params.project, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN])}"/>
      <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: AuthConstants.CTX_APPLICATION, type: AuthConstants.TYPE_PROJECT, name: params.project, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>

      <g:set var="defaultLastLines" value="${g.rConfig(value: "gui.execution.tail.lines.default", type: 'integer')}"/>
      <g:set var="maxLastLines" value="${g.rConfig(value: "gui.execution.tail.lines.max", type: 'integer')}"/>


      <asset:javascript src="execution/show.js"/>

      <g:embedJSON id="execInfoJSON" data="${[jobId:scheduledExecution?.extid,execId:execution.id]}"/>
      <g:embedJSON id="jobDetail"
                   data="${[id: scheduledExecution?.extid, name: scheduledExecution?.jobName, group: scheduledExecution?.groupPath,
                            project: params.project ?: request.project]}"/>
      <g:embedJSON id="workflowDataJSON" data="${workflowTree}"/>
      <g:embedJSON id="nodeStepPluginsJSON" data="${stepPluginDescriptions.node.collectEntries { [(it.key): [title: it.value.title]] }}"/>
      <g:embedJSON id="wfStepPluginsJSON" data="${stepPluginDescriptions.workflow.collectEntries { [(it.key): [title: it.value.title]] }}"/>
      <g:if test="${grails.util.Environment.current==grails.util.Environment.DEVELOPMENT}">
          <asset:javascript src="workflow.test.js"/>
          <asset:javascript src="util/compactMapList.test.js"/>
      </g:if>
      <g:jsMessages codes="['execution.show.mode.Log.title','execution.page.show.tab.Nodes.title']"/>

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
      <style type="text/css">
        #log{
            margin-bottom:20px;
        }
        .padded{
            padding: 10px;
        }
        .errmsg {
            color: gray;
        }
        .executionshow.affix:before,
        .executionshow.affix:after {
            content: " ";
            display: table;
        }
        .executionshow.affix:after {
            clear: both;
        }
        .executionshow .runoutput {
            display: none;
        }
        .executionshow.affix .runoutput {
            display: block;
        }
        .executionshow.affix {
            top: 0;
            width: 80%;
            z-index: 1;
            margin-right: auto;
            margin-left: auto;
            padding-left: 15px;
            padding-right: 15px;
            padding-top: 20px;
            padding-bottom: 10px;
            border-bottom: 1px solid #eeeeee;
        }
        .executionshow.affix.panel-heading-affix {
            background-color: #eeeeee;
            width: auto;
            margin: 0 15px;
            padding: 8px 10px;
        }
        .affixed-shown {
            display: none;
        }
        .affix .affixed-shown {
            display: block;
            margin-top: 0px;
            margin-left: 15px;
        }
        .affix .affixed-shown.affixed-shown-inline {
            display: inline;
        }
      </style>
      <g:set var="projectName" value="${execution.project}"/>
      <g:javascript>
    var execInfo=loadJsonData('execInfoJSON');
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
            filterListUrl:"${enc(js:g.createLink(controller:'reports',action: 'listFiltersAjax', params: [project:projectName]))}",
            filterSaveUrl:"${enc(js:g.createLink(controller:'reports',action: 'saveFilterAjax', params: [project:projectName]))}",
            filterDeleteUrl:"${enc(js:g.createLink(controller:'reports',action: 'deleteFilterAjax', params: [project:projectName]))}",
            pagination:{
                max: ${enc(js:params.max?params.int('max',10):10)}
          },
          query:{
              jobIdFilter:execInfo.jobId
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

      <asset:stylesheet href="static/css/chunk-vendors.css"/>
      <asset:stylesheet href="static/css/pages/execution-show.css"/>
      <asset:javascript src="static/pages/execution-show.js" defer="defer"/>
  </head>
  <g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
  <body id="executionShowPage">


    <div class="content">
    <div id="layoutBody">
        <div class="container-fluid">

              <nav id="subtitlebar" class=" subtitlebar has-content execution-page">
                <div class="subtitle-head flex-container reverse flex-align-items-stretch" data-ko-bind="nodeflow">
                    <div class="subtitle-head-item execution-head-info flex-item-1">
                        <section class="flex-container reverse">
                            <section class="flex-item-1 text-right">
                            <div style="display:inline-block;vertical-align:bottom;margin-right:.3em;">
                              <g:render template="/scheduledExecution/showExecutionLink"
                                          model="[scheduledExecution: scheduledExecution,
                                                  linkCss           : 'text-h4',
                                                  noimgs            : true,
                                                  execution         : execution,
                                                  hideExecStatus    : true,
                                                  followparams      : [mode: followmode, lastlines: params.lastlines]
                                          ]"/>
                            </div>

                                <g:if test="${deleteExecAuth || authChecks[AuthConstants.ACTION_READ]}">
                                    <div class="btn-group" data-bind="visible: completed()">
                                        <button type="button"
                                                class="btn btn-default btn-sm dropdown-toggle"
                                                data-toggle="dropdown"
                                                aria-expanded="false">
                                            <i class="glyphicon glyphicon-list"></i>
                                            <span class="caret"></span>
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-right" role="menu">
                                            <g:if test="${eprev}">
                                              <li>
                                                <g:link action="show" controller="execution" id="${eprev.id}"
                                                      params="[project: eprev.project]"
                                                      title="Previous Execution #${eprev.id}">
                                                  <i class="glyphicon glyphicon-arrow-left"></i>
                                                  <g:message code="previous.execution"/>
                                                </g:link>
                                              </li>
                                            </g:if>

                                            <g:if test="${enext}">
                                              <li>
                                                <g:link action="show" controller="execution"
                                                    title="Next Execution #${enext.id}"
                                                    params="[project: enext.project]"
                                                    id="${enext.id}">
                                                    <i class="glyphicon glyphicon-arrow-right"></i>
                                                    <g:message code="next.execution"/>
                                                </g:link>
                                              </li>
                                            </g:if>
                                            <g:if test="${deleteExecAuth}">
                                                <li>
                                                    <a href="#execdelete"
                                                      data-toggle="modal">
                                                        <i class="fas fa-trash"></i>
                                                        <g:message code="button.action.delete.this.execution"/>
                                                    </a>
                                                </li>
                                            </g:if>

                                            <g:if test="${authChecks[AuthConstants.ACTION_READ]}">
                                                <li class="divider  ">

                                                </li>
                                                <li>
                                                    <a type="button" href="#details_modal" data-toggle="modal">
                                                        <g:icon name="info-sign"/>
                                                        <g:message code="definition"/>
                                                    </a>
                                                </li>

                                            </g:if>
                                        </ul>
                                    </div>
                                </g:if>




                            </section>
                            <section class="flex-item-2">

                                <section class="section-space">

                                    %{-- end of ifScheduledExecutions --}%
                                    <tmpl:wfstateSummaryLine/>

                                </section>


                                <g:if test="${execution.retryAttempt}">
                                    <section class="text-secondary section-space">
                                        <i class="glyphicon glyphicon-repeat"></i>
                                        <g:message code="execution.retry.info.label"
                                                  args="${[execution.retryAttempt, execution.retry]}"/>
                                    </section>
                                </g:if>
                            </section>
                        </section>

                        <section class="section-space execution-action-links " style="padding-top:.6em;">

                                <g:if test="${null == execution.dateCompleted}">
                                    <span data-bind="if: canKillExec()">
                                        <span data-bind="visible: !completed() ">
                                            <!-- ko if: !killRequested() || killStatusFailed() -->
                                            <span class="btn btn-sm btn-danger pull-right"
                                                  data-bind="click: killExecAction">
                                                <g:message code="button.action.kill.job"/>
                                                <i class="glyphicon glyphicon-remove"></i>
                                            </span>
                                            <!-- /ko -->
                                            <!-- ko if: killRequested() -->
                                            <!-- ko if: killStatusPending() -->
                                            <g:img class="loading-spinner" file="spinner-gray.gif" width="16px"
                                                  height="16px"/>
                                            <!-- /ko -->
                                            <span class="loading" data-bind="text: killStatusText"></span>
                                            <!-- /ko -->
                                            <!-- ko if: killedbutNotSaved() -->
                                            <span class="btn btn-danger btn-xs pull-right"
                                                  data-bind="click: markExecAction">
                                                <g:message code="button.action.incomplete.job" default="Mark as Incomplete"/>
                                                <i class="glyphicon glyphicon-remove"></i>
                                            </span>
                                            <!-- /ko -->
                                        </span>
                                    </span>
                                </g:if>
                            <g:if test="${scheduledExecution}">
                                    <g:if test="${authChecks[AuthConstants.ACTION_RUN] && g.executionMode(
                                            active: true,
                                            project: execution.project
                                    )}">
                                    %{--Run again link--}%
                                        <g:link controller="scheduledExecution"
                                                action="execute"
                                                id="${scheduledExecution.extid}"
                                                class=" pull-right"
                                                params="${[retryExecId: execution.id, project: execution.project]}"
                                                title="${g.message(code: 'execution.job.action.runAgain')}"
                                                style="${wdgt.styleVisible(
                                                        if: null != execution.dateCompleted &&
                                                            null ==
                                                            execution.failedNodeList
                                                )};"
                                                data-bind="visible: completed() && !failed()">
                                            <g:message code="execution.action.runAgain"/>
                                            <i class="fas fa-redo-alt"></i>
                                        </g:link>
                                    %{--Run again and retry failed links in a dropdown --}%
                                        <div class="btn-group pull-right"
                                            style="${wdgt.styleVisible(
                                                    if: null != execution.dateCompleted &&
                                                        null !=
                                                        execution.failedNodeList
                                            )};"
                                            data-bind="visible: failed()">
                                            <button class="btn btn-default btn-sm dropdown-toggle"
                                                    data-target="#"
                                                    data-toggle="dropdown">
                                                <g:message code="execution.action.runAgain.ellipsis"/>
                                                <i class="caret"></i>
                                            </button>
                                            <ul class="dropdown-menu pull-left" role="menu">
                                                <li class="retrybuttons">
                                                    <g:link controller="scheduledExecution"
                                                            action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'execution.job.action.runAgain')}"
                                                            data-bind="visible: completed()">
                                                        <b class="glyphicon glyphicon-play"></b>


                                                        <g:message code="execution.action.runAgain"/>
                                                    </g:link>
                                                </li>
                                                <li class="divider">

                                                </li>
                                                <li class="retrybuttons">
                                                    <g:link controller="scheduledExecution" action="execute"
                                                            id="${scheduledExecution.extid}"
                                                            params="${[retryFailedExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'retry.job.failed.nodes')}">
                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>
                                                    </g:link>
                                                </li>

                                            %{--                              todo extra actions--}%

                                                <g:ifMenuItems type="EXECUTION_RETRY"  project="${params.project}" execution="${execution.id.toString()}">
                                                    <li role="separator" class="divider"></li>
                                                    <g:forMenuItems type="EXECUTION_RETRY" var="item"  project="${params.project}" execution="${execution.id.toString()}">
                                                        <li>
                                                            <a href="${enc(attr:item.getExecutionHref(params.project, execution.id.toString()))}"
                                                              title="${enc(attr:g.message(code:item.titleCode,default:item.title))}">
                                                                <span class="sidebar-mini"><i class="${enc(attr: item.iconCSS ?: 'fas fa-plug')}"></i></span>
                                                                <span class="sidebar-normal">
                                                                    <g:message code="${item.titleCode}" default="${item.title}"/>
                                                                </span>
                                                            </a>
                                                        </li>
                                                    </g:forMenuItems>
                                                </g:ifMenuItems>
                                            </ul>
                                        </div>

                                    </g:if>

                                </g:if>
                                <g:if test="${isAdhoc}">
                                %{--run again links--}%
                                    <g:if test="${adhocRunAllowed && g.executionMode(
                                            active: true,
                                            project: execution.project
                                    )}">
                                    %{--run again only--}%
                                        <g:link
                                                controller="framework"
                                                action="adhoc"
                                                params="${[fromExecId: execution.id, project: execution.project]}"
                                                title="${g.message(code: 'execution.action.runAgain')}"
                                                class="  pull-right"
                                                style="${wdgt.styleVisible(
                                                        if: null != execution.dateCompleted &&
                                                            null ==
                                                            execution.failedNodeList
                                                )}"
                                                data-bind="visible: completed() && !failed()">

                                            <g:message code="execution.action.runAgain"/>
                                            <i class="fas fa-redo-alt"></i>
                                        </g:link>
                                    %{--run again and retry failed --}%
                                        <div class="btn-group pull-right"
                                            style="${wdgt.styleVisible(
                                                    if: null != execution.dateCompleted &&
                                                        null !=
                                                        execution.failedNodeList
                                            )}"
                                            data-bind="visible: failed()">
                                            <button class="btn btn-default btn-sm dropdown-toggle "
                                                    data-target="#"
                                                    data-toggle="dropdown">
                                                <g:message code="execution.action.runAgain.ellipsis"/>
                                                <i class="caret"></i>
                                            </button>
                                            <ul class="dropdown-menu pull-right" role="menu">

                                                <li>
                                                    <g:link
                                                            controller="framework"
                                                            action="adhoc"
                                                            params="${[fromExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'execution.action.runAgain')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="execution.action.runAgain"/>&hellip;
                                                    </g:link>
                                                </li>
                                                <li class="divider  ">

                                                </li>
                                                <li>
                                                    <g:link
                                                            controller="framework"
                                                            action="adhoc"
                                                            params="${[retryFailedExecId: execution.id, project: execution.project]}"
                                                            title="${g.message(code: 'retry.failed.nodes.description')}">

                                                        <b class="glyphicon glyphicon-play"></b>
                                                        <g:message code="retry.failed.nodes"/>&hellip;
                                                    </g:link>
                                                </li>
                                            </ul>
                                        </div>
                                    </g:if>
                                </g:if>

                        </section>

                    </div>

                    <div class="subtitle-head-item execution-aux-info flex-item-1">
                        <section>
                            <g:if test="${isAdhoc}">
                                <div class="text-h5">
                                    <b class="exec-status icon "
                                      data-bind="attr: { 'data-execstate': executionState, 'data-statusstring':executionStatusString }">
                                    </b>
                                    <g:render template="wfItemView" model="[
                                            item: execution.workflow.commands[0],
                                            icon: 'icon-small'
                                    ]"/>
                                </div>
                            </g:if>
                            <g:if test="${scheduledExecution}">
                                <g:render template="/scheduledExecution/showHead"
                                          model="[scheduledExecution: scheduledExecution,
                                                  includeExecStatus : true,
                                                  jobDescriptionMode: 'expanded',
                                                  jobActionButtons  : true,
                                                  linkCss           : 'text-h4',
                                                  scmExportEnabled  : scmExportEnabled,
                                                  scmExportStatus   : scmExportStatus,
                                                  scmImportEnabled  : scmImportEnabled,
                                                  scmImportStatus   : scmImportStatus
                                          ]"/>

                                <g:if test="${execution.argString}">
                                    <section class=" section-space exec-args-section argstring-scrollable">
                                        <span class="text-secondary"><g:message code="options.prompt"/></span class="text-secondary">
                                        <g:render template="/execution/execArgString"
                                                  model="[argString: execution.argString, inputFilesMap: inputFilesMap]"/>
                                    </section>
                                </g:if>



                            </g:if>

                        </section>

                    </div>
                </div>

                <div class="" data-bind="if: !completed() " data-ko-bind="nodeflow">
                    <g:if test="${scheduledExecution}">
                    %{--progress bar--}%
                        <div>
                            <section
                                    data-bind="if: !completed() && !queued() && jobAverageDuration()>0">
                                <g:set var="progressBind"
                                      value="${', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() > 104  }'}"/>
                                <g:render template="/common/progressBar"
                                          model="[completePercent : execution.dateCompleted ? 100 : 0,
                                                  progressClass   : 'rd-progress-exec progress-embed progress-square',
                                                  progressBarClass: '',
                                                  containerId     : 'progressContainer2',
                                                  innerContent    : '',
                                                  showpercent     : true,
                                                  height          : 28,
                                                  progressId      : 'progressBar',
                                                  bind            : 'jobPercentageFixed()',
                                                  bindText        : '(jobPercentageFixed()  < 105 ? jobPercentageFixed() + \'%\' : \'+\' + jobOverrunDuration()) + \' of average \' + formatDurationHumanize(jobAverageDuration())',
                                                  progressBind    : progressBind,
                                          ]"/>
                            </section>
                            <section data-bind="if: queued()">
                                <div  class="progress progress-striped" style="height: 28px">
                                    <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100"
                                        style="width: 100%;line-height: 28px">
                                        Queued
                                    </div>
                                </div>
                            </section>
                        </div>
                    </g:if>
                    <g:if test="${isAdhoc}">

                    %{--progress bar--}%
                        <div>
                            <section>
                                <g:render template="/common/progressBar"
                                          model="[completePercent : 100,
                                                  indefinite      : true,
                                                  progressClass   : 'rd-progress-exec progress-embed progress-square',
                                                  progressBarClass: '',
                                                  containerId     : 'progressContainer2',
                                                  innerContent    : '',
                                                  showpercent     : false,
                                                  height          : 28,
                                                  progressId      : 'progressBar',

                                          ]"/>
                            </section>

                        </div>
                    </g:if>
                </div>
              </nav>


              <div class="row">
                  <div class="col-sm-12">
                      <div class="card card-plain " data-ko-bind="nodeflow">
                          <div class="btn-group " data-bind="if: views().length>2">
                              <button class="btn btn-default btn-sm dropdown-toggle "
                                      id="views_dropdown_button"
                                      data-target="#"
                                      data-toggle="dropdown">
                                  <span class="colon-after"><g:message code="view"/></span>
                                  <span data-bind="text: activeTabData() && activeTabData().title">

                                  </span>
                                  <i class="caret"></i>
                              </button>
                              <ul class="dropdown-menu pull-left" role="menu" data-bind="foreach: views">

                                  <li data-bind="attr: {id: 'tab_link_'+id }">
                                      <a href="#"
                                         data-bind="click: function(){$root.activeTab(id)}, attr: {href: '#'+id }">
                                          <span data-bind="text: title"></span>
                                          <!-- ko if: $root.activeTab()===id -->
                                          <i class="fas fa-check" style="margin-left:1em"></i>
                                          <!-- /ko -->
                                      </a>
                                  </li>

                              </ul>

                          </div>
                          <!-- ko foreach: viewButtons -->
                          <a href="#"
                             data-bind="click:function(){$root.activeTab(id)}, attr: {href: '#'+id, id: 'btn_view_'+id }, visible: $root.activeTab()!==id"
                             class="btn btn-sm">
                              <span data-bind="text: title"></span> &raquo;
                          </a>
                          <!-- /ko -->


                          <span data-bind="visible: activeTab().startsWith('output')">


                              <span data-bind="visible: completed()" class="execution-action-links pull-right">

                                  <span class="btn-group">
                                      <button type="button" class="btn btn-default btn-xs dropdown-toggle"
                                              data-toggle="dropdown">
                                          <g:message code="execution.log" />
                                          <span class="caret"></span>
                                      </button>
                                      <ul class="dropdown-menu pull-right" role="menu">
                                          <li>
                                              <g:link class=""
                                                      title="${message(
                                                              code: 'execution.show.log.text.button.description',
                                                              default: 'View text output'
                                                      )}"
                                                      controller="execution"
                                                      action="downloadOutput"
                                                      id="${execution.id}"
                                                      params="[
                                                              view     : 'inline',
                                                              formatted: false,
                                                              project  : execution.project,
                                                              stripansi: true
                                                      ]"
                                                      target="_blank">

                                                  <g:message code="execution.show.log.text.button.title"/>
                                              </g:link>
                                          </li>
                                          <li>

                                              <g:link class=""
                                                      title="${message(
                                                              code: 'execution.show.log.html.button.description',
                                                              default: 'View rendered output'
                                                      )}"
                                                      controller="execution"
                                                      action="renderOutput"
                                                      id="${execution.id}"
                                                      params="[
                                                              project: execution.project,
                                                              ansicolor: 'on',
                                                              loglevels: 'on',
                                                              convertContent: 'on'
                                                      ]"
                                                      target="_blank">

                                                  <g:message code="execution.show.log.html.button.title"/>
                                              </g:link>
                                          </li>
                                          <li role="separator" class="divider"></li>
                                          <li class="dropdown-header">
                                              <g:message code="execution.show.log.download.button.title"/>
                                          </li>
                                          <li>
                                              <g:link class="_guess_tz_param"
                                                      data-tz-url-param="timeZone"
                                                      title="${message(
                                                              code: 'execution.show.log.download.button.description',
                                                              default: 'Download {0} bytes',
                                                              args: [filesize > 0 ? filesize : '?']
                                                      )}"
                                                      controller="execution"
                                                      action="downloadOutput"
                                                      id="${execution.id}"
                                                      params="[project: execution.project]"
                                                      target="_blank">

                                                  <b class="glyphicon glyphicon-download"></b>
                                                  <g:message code="formatted.text" />
                                              </g:link>
                                          </li>
                                      </ul>
                                  </span>

                              </span>

                              <g:render template="/common/modal"
                                        model="[modalid   : 'view-options-modal',
                                                titleCode : 'execution.page.view.options.title',
                                                cancelCode: 'close']">
                                  <div class="container form-horizontal">

                                      <div class="form-group">
                                          <label class="col-sm-2 control-label" for="view-option-style-mode">
                                              Style
                                          </label>

                                          <div class="col-sm-10">

                                              <select data-bind="options: logoutput().options.styleModesAvailable, value:logoutput().options.styleMode"
                                                      class="form-control"
                                                      id="view-option-style-mode">

                                              </select>

                                          </div>
                                      </div>


                                      <div class="form-group">

                                          <label class="col-sm-2 control-label">
                                              Text
                                          </label>
                                          <div class="col-sm-10">
                                              <div class="checkbox">
                                                  <input type="checkbox"
                                                         data-bind="checked: logoutput().options.showAnsicolor"
                                                         id="view-option-ansi-color"/>
                                                  <label for="view-option-ansi-color">
                                                      <g:message code="execution.show.mode.ansicolor.title"
                                                                 default="Ansi Color"/>
                                                  </label>
                                              </div>
                                          </div>

                                          <div class="col-sm-offset-2 col-sm-10">
                                              <div class="checkbox">
                                                  <input type="checkbox"
                                                         data-bind="checked: logoutput().options.wrapLines"
                                                         id="view-option-wrap-lines"/>
                                                  <label for="view-option-wrap-lines">
                                                      <g:message code="execution.show.mode.wrapmode.title"
                                                                 default="Wrap Long Lines"/>
                                                  </label>
                                              </div>
                                          </div>

                                          <div class="col-sm-offset-2 col-sm-10">
                                              <div class="checkbox">
                                                  <input type="checkbox"
                                                         data-bind="checked: logoutput().options.followmodeNode"
                                                         id="view-option-node-view"/>
                                                  <label for="view-option-node-view">
                                                      <g:message code="execution.show.mode.Compact.title"
                                                                 default="Compact"/>
                                                  </label>
                                              </div>
                                          </div>

                                      </div>

                                      <div class="form-group">

                                          <label class="col-sm-2 control-label">Columns</label>

                                          <div class="col-sm-10">

                                              <div>

                                                  <div class="checkbox-inline">
                                                      <input type="checkbox"
                                                             value="true"
                                                             data-bind="checked: logoutput().options.showTime"
                                                             id="view-option-show-time"/>
                                                      <label for="view-option-show-time">
                                                          <g:message code="execution.show.mode.column.time" />
                                                      </label>
                                                  </div>

                                                  <div class="checkbox-inline">
                                                      <input type="checkbox"
                                                             value="true"
                                                             data-bind="checked: logoutput().options.showNodeCol"
                                                             id="view-option-show-node"/>
                                                      <label for="view-option-show-node">
                                                          <g:message code="execution.show.mode.column.node" />
                                                      </label>
                                                  </div>

                                                  <div class="checkbox-inline">
                                                      <input type="checkbox"
                                                             value="true"
                                                             data-bind="checked: logoutput().options.showStep"
                                                             id="view-option-show-step"/>
                                                      <label for="view-option-show-step">
                                                          <g:message code="execution.show.mode.column.step" />
                                                      </label>
                                                  </div>
                                              </div>

                                          </div>
                                      </div>


                                      <div class="form-group">
                                          <label class="col-sm-2 control-label">
                                              <g:message code="execution.show.mode.inset.label" />
                                          </label>
                                          <div class="col-sm-10">
                                              <div class="checkbox">
                                                  <input type="checkbox"
                                                         data-bind="checked: logoutput().options.showNodeInset"
                                                         id="view-option-node-inset"/>
                                                  <label for="view-option-node-inset">
                                                      <g:message code="execution.show.mode.inset.node" />
                                                  </label>
                                              </div>
                                          </div>

                                      </div>

                                  </div>

                              </g:render>

                          </span>

                      </div>

                      <div class="card exec-output "
                           data-ko-bind="nodeflow"
                           data-mode="normal"
                           data-bind="attr: {'data-mode': logoutput().options.styleMode }, css: {'exec-output-bg': activeTab()==='output' }">

                          <div class="card-content " data-bind="css: {tight: activeTab().startsWith('output') }">
                              <g:render template="/common/messages"/>


                              <div class="tab-content" id="exec-main-view">

                                  <!-- ko foreach: contentViews -->
                                  <div class="tab-pane" data-bind="css: {active: $root.activeTab()===id}, attr: {id:id}">
                                      <span data-bind="attr: {id:id+'_content'}, html:content"></span>
                                  </div>
                                  <!-- /ko -->
                                  <div class="tab-pane " id="nodes" data-bind="css: {active: activeTab()==='nodes'}">
                                      <div class="flowstate ansicolor ansicolor-on" id="nodeflowstate">
                                          <g:render template="wfstateNodeModelDisplay" bean="${workflowState}"
                                                    var="workflowState"/>
                                      </div>
                                  </div>

                                  <div style="height: calc(100vh - 250px); display: none; contain: layout;"
                                       id="output"
                                       class="card-content-full-width"
                                       data-bind="visible: activeTab() === 'output' || activeTab().startsWith('outputL')"
                                  >
                                      <div class="execution-log-viewer" data-execution-id="${execution.id}" data-theme="light" data-follow="true"></div>
                                  </div>

                              </div>
                          </div>

                          <g:if test="${authChecks[AuthConstants.ACTION_READ]}">
                              <g:render template="/common/modal"
                                        model="[
                                                modalid   : 'details_modal',
                                                modalsize : 'modal-lg',
                                                title     : message(code: 'definition'),
                                                cancelCode: 'close',
                                                links     : isAdhoc ? [
                                                        [
                                                                messageCode: 'execution.action.saveAsJob.ellipsis',
                                                                href       : createLink(
                                                                        controller: 'scheduledExecution',
                                                                        action: 'createFromExecution',
                                                                        params: [executionId: execution.id, project: execution.project]
                                                                ),
                                                                bind       : 'visible: completed()',
                                                                css        : 'btn-success'
                                                        ]
                                                ] : []
                                        ]">

                                  <div>
                                      <g:render template="execDetails"
                                                model="[execdata: execution, showArgString: false, hideAdhoc: false, isScheduled:isScheduled]"/>
                                  </div>

                              </g:render>

                          </g:if>
                    </div>
                  </div>
          <g:if test="${scheduledExecution}">

              <g:set var="hasEventReadAuth" value="${auth.resourceAllowedTest(
                      project: scheduledExecution.project,
                      action: AuthConstants.ACTION_READ,
                      kind: AuthConstants.TYPE_EVENT
              )}"/>
              <div class="col-sm-12">

                  <div class="card" id="activity_section">
                      <div class="card-content">

                          <div class="vue-tabs">
                              <div class="nav-tabs-navigation">
                                  <div class="nav-tabs-wrapper">
                                      <ul class="nav nav-tabs activity_links">
                                          <li class="active">
                                              <a href="#stats" data-toggle="tab"><g:message code="job.view.stats.label" /></a>
                                          </li>
                                          <g:if test="${hasEventReadAuth}">
                                              <li>
                                                  <a href="#history" data-toggle="tab"><g:message code="page.section.Activity" /></a>
                                              </li>
                                          </g:if>
                                      </ul>
                                  </div>
                              </div>
                              <div class="tab-content">
                                  <div class="tab-pane active" id="stats">


                                      <section class="_jobstats_content section-space-bottom-lg container-fluid" id="_job_stats_main">
                                          <g:render template="/scheduledExecution/renderJobStats"
                                                    model="${[scheduledExecution: scheduledExecution]}"/>
                                      </section>


                                      <div id="_job_stats_extra_placeholder"></div>
                                  </div>
                                  <g:if test="${hasEventReadAuth}">
                                      <div class="tab-pane" id="history">

                                          <div data-ko-bind="history" class="_history_content vue-project-activity">

                                              <activity-list :event-bus="EventBus"></activity-list>
                                          </div>
                                      </div>
                                  </g:if>
                              </div>
                          </div>


                      </div>
                  </div>
              </div>

          </g:if>
    </div>


  </div>
    </div>
    </div>
  <g:render template="/menu/copyModal"
          model="[projectNames: projectNames]"/>

  %{--delete execution modal--}%
  <g:if test="${deleteExecAuth}">
    <div class="modal" id="execdelete" tabindex="-1" role="dialog" aria-labelledby="deleteexectitle" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
            <h4 class="modal-title" id="deleteexectitle"><g:message code="delete.execution.title" /></h4>
          </div>

          <div class="modal-body">
            <p class=" "><g:message code="really.delete.this.execution" /></p>
          </div>
          <div class="modal-footer">
            <g:form controller="execution" action="delete" method="post" useToken="true">
              <g:hiddenField name="id" value="${execution.id}"/>
              <button type="submit" class="btn btn-default btn-xs " data-dismiss="modal">
                <g:message code="cancel" />
              </button>
              <input type="submit" value="${g.message(code:'button.action.Delete')}" class="btn btn-danger btn-xs"/>
            </g:form>
          </div>
        </div>
      </div>
    </div>
  </g:if>
  %{--/delete execution modal--}%


  <script type="text/html" id="step-info-simple">
    %{--Display the lowest level step info: [icon] identity --}%
        <i class="rdicon icon-small" data-bind="css: stepinfo.type"></i>
        <span data-bind="text: stepinfo.stepident"></span>
  </script>
  <script type="text/html" id="step-info">
    %{--wrap step-info-simple in tooltip --}%
    <span data-bind="attr: {title: stepinfo.stepctxPathFull}, bootstrapTooltip: stepinfo.stepctxPathFull" data-placement="top" data-container='body'>
        <span data-bind="template: { name: 'step-info-simple', data:stepinfo, as: 'stepinfo' }"></span>
    </span>
  </script>
  <script type="text/html" id="step-info-simple-link">
    %{--wrap step-info-simple in tooltip --}%
    <span data-bind="if: stepinfo.hasLink()">
        <a data-bind="urlPathParam: stepinfo.linkJobId(), attr: {title: 'Click to view Job: '+stepinfo.linkTitle() }"
           href="${createLink(
                controller: 'scheduledExecution',
                action: 'show',
                params: [project: execution.project, id: '<$>']
        )}">
            <span data-bind="template: { name: 'step-info-simple', data:stepinfo, as: 'stepinfo' }"></span>
        </a>
    </span>
    <span data-bind="if: !stepinfo.hasLink()">
        <span data-bind="template: { name: 'step-info-simple', data:stepinfo, as: 'stepinfo' }"></span>
    </span>
  </script>
  <script type="text/html" id="step-info-path">
    %{-- Display the full step path with icon and identity --}%
    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-strong"/>

    </span>
    <span data-bind="template: { name: 'step-info-simple', data:stepinfo, as: 'stepinfo' }"></span>
  </script>
  <script type="text/html" id="step-info-path-links">
    %{-- Display the full step path with icon and identity --}%
    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path-links', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-strong"/>

    </span>
    <span data-bind="template: { name: 'step-info-simple-link', data:stepinfo, as: 'stepinfo' }"></span>
  </script>
  <script type="text/html" id="step-info-parent-path">
    %{-- Display the full step path with icon and identity --}%

    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-strong"/>
    </span>
  </script>
  <script type="text/html" id="step-info-parent-path-links">
    %{-- Display the full step path with icon and identity --}%

    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path-links', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-strong"/>
    </span>
  </script>

  <script type="text/html" id="step-info-path-base">
    %{-- Display the full step path with icon and identity --}%
    <span data-bind="template: { name: 'step-info-parent-path', data:stepinfo, as: 'stepinfo' }"></span>

    <span data-bind="template: { name: 'step-info', data:stepinfo, as: 'stepinfo' }"></span>
  </script>

  <script type="text/html" id="step-info-extended">
  %{--Display the lowest level extended info:  [icon] number. identity --}%
    <span data-bind="attr: {title: stepinfo.stepctxPathFull}, bootstrapTooltip: stepinfo.stepctxPathFull" data-placement="top" data-container='body'>
    <i class="rdicon icon-small" data-bind="css: stepinfo.type"></i>
    <span data-bind="text: stepinfo.stepdesc"></span>
    </span>
  </script>
  <g:jsonToken id="exec_cancel_token" url="${request.forwardURI}"/>
  <!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
        <script type="application/javascript">
    var workflow=null;
    var followControl=null;
    var flowState=null;
    var nodeflowvm=null;
    var logoutput=null;
    function followOutput(){
        nodeflowvm.logoutput().beginFollowingOutput('${enc(js: execution?.id)}');
    }
    function followState(){
        try{
            flowState.beginFollowing();
        }catch(e){
            nodeflowvm.errorMessage('Could not load flow state: '+e);
            nodeflowvm.stateLoaded(false);
        }
    }

    var activity;
    function init() {
        var execInfo=loadJsonData('execInfoJSON');
        var workflowData=loadJsonData('workflowDataJSON');
        RDWorkflow.nodeSteppluginDescriptions=loadJsonData('nodeStepPluginsJSON');
        RDWorkflow.wfSteppluginDescriptions=loadJsonData('wfStepPluginsJSON');
        workflow = new RDWorkflow(workflowData);

      var multiworkflow=new MultiWorkflow(workflow,{
            dynamicStepDescriptionDisabled:${enc(js:feature.isDisabled(name:'workflowDynamicStepSummaryGUI'))},
            url:appLinks.scheduledExecutionWorkflowJson,
            id:execInfo.jobId||execInfo.execId,//id of job or execution
            workflow:workflowData
        });
      followControl = new FollowControl('${execution?.id}','outputappendform',{
        parentElement:'commandPerform',
        fileloadId:'fileload',
        fileloadPctId:'fileloadpercent',
        fileloadProgressId:'fileloadprogress',
        cmdOutputErrorId:'cmdoutputerror',
        outfileSizeId:'outfilesize',
        workflow:workflow,
        multiworkflow:multiworkflow,
        appLinks:appLinks,

        extraParams:"<%="true" == params.disableMarkdown ? '&disableMarkdown=true' : ''%>&markdown=${enc(js:enc(url: params.markdown))}&ansicolor=${enc(js:enc(url: params.ansicolor))}&renderContent=${enc(js:enc(url: params.renderContent))}",
        lastlines: '${enc(js:params.int('lastlines') ?: defaultLastLines)}',
        maxLastLines:'${enc(js:params.int('maxlines') ?: maxLastLines)}',
        collapseCtx: {value:${enc(js:null == execution?.dateCompleted)},changed:false},
        showFinalLine: {value:false,changed:false},
        tailmode: ${enc(js:followmode == 'tail')},
        browsemode: ${enc(js:followmode == 'browse')},
        nodemode: ${enc(js:followmode == 'node')},
        execData: {},
        groupOutput:{value:${enc(js:followmode == 'browse')}},
        updatepagetitle:${enc(js:null == execution?.dateCompleted)},
        killjobauth:${enc(js: authChecks[AuthConstants.ACTION_KILL] ? true : false)},
      <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
          killjobhtml: '<span class="btn btn-danger btn-xs textbtn" onclick="followControl.docancel();">Kill <g:message code="domain.ScheduledExecution.title"/> <i class="glyphicon glyphicon-remove"></i></span>',
      </g:if>
      <g:if test="${!authChecks[AuthConstants.ACTION_KILL]}">
          killjobhtml: "",
      </g:if>
        totalDuration : '${enc(js:scheduledExecution?.getTotalTimeStats()?: -1)}',
        totalCount: '${enc(js: scheduledExecution?.getExecCountStats() ?: -1)}',
        colStep:{value:${enc(js: !isAdhoc)} },
        colNode:{value:false}
      });
      nodeflowvm=new NodeFlowViewModel(
        workflow,
        "${enc(js:g.createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id,params:[format:'json']))}",
        "${enc(js:g.createLink(controller: 'execution', action: 'ajaxExecNodeState', id: execution.id))}",
        multiworkflow,
        {
            followControl:followControl,
            executionId:'${enc(js: execution.id)}',
            logoutput: new LogOutput({
                followControl:followControl,
                bindFollowControl:true,
                options:{
                    followmode:"${enc(js: followmode)}",
                    showStep:${enc(js: !isAdhoc)},
                    showNodeCol:false,
                }
            } ),
            views: [
                {id: 'nodes', title: message('execution.page.show.tab.Nodes.title'), showButton: true},
                {id: 'output', title: message('execution.show.mode.Log.title'), showButton: true}
            ]
        }
        );
        flowState = new FlowState('${enc(js: execution?.id)}','flowstate',{
        workflow:workflow,
        loadUrl: "${enc(js:g.createLink(controller: 'execution', action: 'ajaxExecState', id: execution.id))}",
        outputUrl:"${g.enc(js:createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id,params:[format:'json']))}",
        selectedOutputStatusId:'selectedoutputview',
        reloadInterval:1500,
     });

      nodeflowvm.followFlowState(flowState,true);

        ko.mapping.fromJS({
            completed:'${execution.dateCompleted != null}',
            startTime:'${enc(js:execution.dateStarted)}',
            endTime:'${enc(js:execution.dateCompleted)}',
            executionState:'${enc(js:execution.executionState)}',
            executionStatusString:'${enc(js:execution.status)}'
        },{},nodeflowvm);

        nodeflowvm.selectedNodes.subscribe(function (newValue) {
            if (newValue) {
                flowState.loadUrlParams=jQuery.extend(flowState.loadUrlParamsBase,{nodes:newValue.join(",")});
            }else{
                flowState.loadUrlParams=flowState.loadUrlParamsBase;
            }
        });

        //knockout activeTab change listener to begin output or state listener
        nodeflowvm.activeTab.subscribe(function(val){
            window.location.hash = "#" + val
            if (val === 'nodes') {
                followState();
           }
        });

        let doupdate = true//!nodeflowvm.completed()
        let prefixed=''
        const updateTitle = function (prefix) {
            let title=document.title
            if(prefixed && title.startsWith(prefixed)){
                title=title.substring(prefixed.length)
            }
            document.title = prefix + title;
            prefixed=prefix
        }

        nodeflowvm.executionState.subscribe(function (val) {
            if (val === 'RUNNING' && !doupdate) {
                doupdate = true
            } else if (val === 'RUNNING' && doupdate) {
                doupdate = true

                updateTitle('[RUNNING] ')
            } else if (null != val && val !== 'RUNNING' && doupdate) {
                var prefix = (
                    val === 'SUCCEEDED' ?
                    '✅ [OK] ' :
                    val === 'ABORTED' ?
                    '✖︎ [KILLED] ' :
                    val === 'TIMEDOUT' ?
                    '⏱︎ [TIMEOUT] ' :
                    val === 'FAILED' ?
                    '⛔︎ [FAILED] ' :
                    val === 'QUEUED' ?
                    '🔜 [QUEUED] ' :
                    ('✴️ [' + (val) + '] ')//🔶
                );
                updateTitle(prefix)
            }
        })



        jQuery('.apply_ace').each(function () {
            _applyAce(this);
        });

        PageActionHandlers.registerHandler('copy_other_project',function(el){
            jQuery('#jobid').val(el.data('jobId'));
            jQuery('#selectProject').modal();
            jQuery.ajax({
                dataType:'json',
                method: 'GET',
                url:_genUrl(appLinks.authProjectsToCreateAjax),
                success:function(data){
                    jQuery('#jobProject').empty();
                    for (let i in data.projectNames ) {
                        jQuery('#jobProject').append(
                            '<option value="' + data.projectNames[i] + '">' + data.projectNames[i] + '</option>'
                        );
                    }
                }
            });
        });
        followState();
        var outDetails = window.location.hash;
        if(outDetails.startsWith('#output')) {
            nodeflowvm.activeTab(outDetails.slice(1))
        } else if (outDetails === '#nodes') {
            nodeflowvm.activeTab("nodes");
        }else{
            //default to nodes tab
            nodeflowvm.activeTab("nodes");
        }
        initKoBind(null, {nodeflow: nodeflowvm})
    }
    jQuery(init);
    </script>
  </body>
</html>

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

<%@ page import="grails.util.Environment; rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="events"/>
    <meta name="layout" content="base" />
    <title><g:appTitle/> -
      <g:if test="${null==execution?.dateCompleted}"><g:message code="now.running" /> - </g:if>
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
      <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(context: 'application', type: 'project', name: params.project, action: AuthConstants.ACTION_ADMIN)}"/>
      <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: 'application', type: 'project', name: params.project, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>

      <g:set var="defaultLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.default}"/>
      <g:set var="maxLastLines" value="${grailsApplication.config.rundeck.gui.execution.tail.lines.max}"/>
      <asset:javascript src="workflow.js"/>
      <asset:javascript src="executionControl.js"/>
      <asset:javascript src="executionState.js"/>
      <asset:javascript src="executionState_HistoryKO.js"/>

      <asset:javascript src="prototype-bundle.js"/>
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
      <style type="text/css">
        #log{
            margin-bottom:20px;
        }
        .inline_only {
            display: none;
        }
        .execstate.isnode[data-execstate=RUNNING],.execstate.isnode[data-execstate=RUNNING_HANDLER] {
            background-image: url(${g.resource(dir: 'images',file: 'icon-tiny-disclosure-waiting.gif')});
            padding-right: 16px;
            background-repeat: no-repeat;
            background-position: right 2px;
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
  </head>
  <g:set var="isAdhoc" value="${!scheduledExecution && execution.workflow.commands.size() == 1}"/>
  <body id="executionShowPage">
  <content tag="subtitlecss">execution-page</content>
  <content tag="subtitlesection">

      <div class=" execution_ko subtitle-head flex-container flex-align-items-stretch">
          <div class="subtitle-head-item execution-head-info flex-item-1">
              <section>
                  <section>
                      <span class="jobInfo" id="jobInfo_${execution.id}">
                          <g:render template="/scheduledExecution/showExecutionLink"
                                    model="[scheduledExecution: scheduledExecution,
                                            linkCss           : 'text-h3', noimgs: true, execution: execution, followparams: [mode: followmode, lastlines: params.lastlines]]"/>
                      </span>

                      <g:if test="${null == execution.dateCompleted}">
                          <span data-bind="if: canKillExec()">
                              <span data-bind="visible: !completed() ">
                                  <!-- ko if: !killRequested() || killStatusFailed() -->
                                  <span class="btn btn-sm"
                                        data-bind="click: killExecAction">
                                      <g:message code="button.action.kill.job"/>
                                  </span>
                                  <!-- /ko -->
                                  <!-- ko if: killRequested() -->
                                  <!-- ko if: killStatusPending() -->
                                  <g:img class="loading-spinner" file="spinner-gray.gif" width="16px" height="16px"/>
                                  <!-- /ko -->
                                  <span class="loading" data-bind="text: killStatusText"></span>
                                  <!-- /ko -->
                                  <!-- ko if: killedbutNotSaved() -->
                                  <span class="btn btn-danger btn-xs"
                                        data-bind="click: markExecAction">
                                      <g:message code="button.action.incomplete.job" default="Mark as Incomplete"/>
                                      <i class="glyphicon glyphicon-remove"></i>
                                  </span>
                                  <!-- /ko -->
                              </span>
                          </span>
                      </g:if>
                  %{--job buttons--}%

                      <g:if test="${deleteExecAuth}">

                          <div class="btn-group" data-bind="visible: completed()">
                              <button type="button"
                                      class="btn btn-sm dropdown-toggle"
                                      data-toggle="dropdown"
                                      aria-expanded="false">
                                  <i class="glyphicon glyphicon-list"></i>
                                  <span class="caret"></span>
                              </button>
                              <ul class="dropdown-menu dropdown-menu-right" role="menu">
                                  <li>
                                      <a href="#execdelete"
                                         data-toggle="modal">
                                          <g:message code="button.action.delete.this.execution"/>
                                          <i class="fas fa-trash"></i>
                                      </a>
                                  </li>
                              </ul>
                          </div>
                      </g:if>
                      <g:if test="${isAdhoc}">

                      %{--save as job link--}%
                          <g:if test="${auth.resourceAllowedTest(
                                  kind: 'job',
                                  action: [AuthConstants.ACTION_CREATE],
                                  project: execution.project
                          )}">
                              <g:link
                                      controller="scheduledExecution"
                                      action="createFromExecution"
                                      params="${[executionId: execution.id, project: execution.project]}"
                                      class=" btn btn-sm header execRerun execRetry"
                                      title="${g.message(code: 'execution.action.saveAsJob')}"
                                      style="${wdgt.styleVisible(if: null != execution.dateCompleted)}"
                                      data-bind="visible: completed()">

                                  <g:message code="execution.action.saveAsJob" default="Save as Job"/>
                                  <i class="fas fa-save"></i>
                              </g:link>
                          </g:if>
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
                                      class="btn btn-default btn-sm execRerun"
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
                              <div class="btn-group execRetry"
                                   style="${wdgt.styleVisible(
                                           if: null != execution.dateCompleted &&
                                               null !=
                                               execution.failedNodeList
                                   )};"
                                   data-bind="visible: failed()">
                                  <button class="btn btn-default btn-sm dropdown-toggle"
                                          data-target="#"
                                          data-toggle="dropdown">
                                      <g:message code="execution.action.runAgain"/>
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
                                  </ul>
                              </div>
                          </g:if>

                      </g:if>
                      <g:if test="${isAdhoc}">
                      %{--run again links--}%
                          <g:if test="${adhocRunAllowed && g.executionMode(active: true, project: execution.project)}">
                          %{--run again only--}%
                              <g:link
                                      controller="framework"
                                      action="adhoc"
                                      params="${[fromExecId: execution.id, project: execution.project]}"
                                      title="${g.message(code: 'execution.action.runAgain')}"
                                      class="btn btn-default btn-sm force-last-child execRerun"
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
                              <div class="btn-group execRetry"
                                   style="${wdgt.styleVisible(
                                           if: null != execution.dateCompleted &&
                                               null !=
                                               execution.failedNodeList
                                   )}"
                                   data-bind="visible: failed()">
                                  <button class="btn btn-default btn-sm dropdown-toggle force-last-child"
                                          data-target="#"
                                          data-toggle="dropdown">
                                      <g:message code="execution.action.runAgain"/>
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


                  <g:if test="${execution.retryAttempt}">
                      <section class="text-primary">
                          <i class="glyphicon glyphicon-repeat"></i>
                          Retry #<g:enc>${execution.retryAttempt}</g:enc>  (of <g:enc>${execution.retry}</g:enc>)
                      </section>
                  </g:if>


                  <section class="section-space">

                      %{-- end of ifScheduledExecutions --}%
                      <tmpl:wfstateSummaryLine/>

                  </section>
              </section>
          </div>

          <div class="subtitle-head-item execution-aux-info flex-item-1">
              <section>
                  <g:if test="${isAdhoc}">
                      <div class="text-h5">
                          <g:render template="wfItemView" model="[
                                  item   : execution.workflow.commands[0],
                                  icon   : 'icon-med',
                                  iwidth : '24px',
                                  iheight: '24px',
                          ]"/>
                      </div>
                  </g:if>
                  <g:if test="${scheduledExecution}">
                      <g:render template="/scheduledExecution/showHead"
                                model="[scheduledExecution: scheduledExecution,
                                        jobDescriptionMode: 'expanded',
                                        jobActionButtons  : true,
                                        linkCss           : 'text-h4',
                                        scmExportEnabled  : scmExportEnabled,
                                        scmExportStatus   : scmExportStatus,
                                        scmImportEnabled  : scmImportEnabled,
                                        scmImportStatus   : scmImportStatus
                                ]"/>

                      <g:if test="${execution.argString}">
                          <section class=" section-space argstring-scrollable">
                              <span class="text-secondary"><g:message code="options.prompt"/></span>
                              <g:render template="/execution/execArgString"
                                        model="[argString: execution.argString, inputFilesMap: inputFilesMap]"/>
                          </section>
                      </g:if>

                  %{--progress bar--}%
                      <div data-bind="if: !completed()">
                          <section class="section-space runstatus "
                                   data-bind="if: !completed() && jobAverageDuration()>0">
                              <g:set var="progressBind"
                                     value="${', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() > 104  }'}"/>
                              <g:render template="/common/progressBar"
                                        model="[completePercent : execution.dateCompleted ? 100 : 0,
                                                progressClass   : 'rd-progress-exec progress-embed',
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

                      </div>

                  </g:if>
              </section>

          </div>
      </div>

  </content>
    <div class="container-fluid">
      <div>

              <div class="row">
          <div class="${isAdhoc ? 'col-sm-12' : 'col-sm-8'}">
                    <div class="card execution_ko">
                      <div class="card-content">
                        <g:render template="/common/messages"/>
                        <g:if test="${isAdhoc}">
                            <section class="section-space-lg section-space-bottom-lg">
                                <g:render template="wfstateSummaryDisplay" bean="${workflowState}" var="workflowState"/>
                            </section>
                        </g:if>
                        <ul class="nav nav-tabs">

                            <li id="tab_link_flow active">
                                <a href="#state" data-toggle="tab" data-bind="text: completed()?'${enc(attr:g.message(code: "report"))}':'${enc(attr:g.message(code: "monitor"))}' ">
                                    <g:if test="${execution.dateCompleted==null}">
                                        <g:message code="monitor" />
                                    </g:if>
                                    <g:else>
                                        <g:message code="report" />
                                    </g:else>
                                </a>
                            </li>

                            <!-- <li id="tab_link_summary">
                                <a href="#summary" data-toggle="tab"><g:message code="execution.page.show.tab.Summary.title" /></a>
                            </li> -->
                            <li id="tab_link_output">
                                <a href="#output" data-toggle="tab"><g:message code="execution.show.mode.Log.title" /></a>
                            </li>
                            <g:if test="${authChecks[AuthConstants.ACTION_READ]}">
                                <li id="tab_link_definition">
                                    <a href="#schedExDetails${scheduledExecution?.id}" data-toggle="tab"><g:message code="definition" /></a>
                                </li>
                            </g:if>
                        </ul>

                        <div class="tab-content">

                          <div class="tab-pane active" id="state">
                              <div class="flowstate ansicolor ansicolor-on" id="nodeflowstate">
                                 <g:render template="wfstateNodeModelDisplay" bean="${workflowState}" var="workflowState"/>
                              </div>
                          </div>
                            <div class="tab-pane " id="output">
                                <g:render template="/execution/showFragment"
                                          model="[execution: execution, scheduledExecution: scheduledExecution, inlineView: false, followmode: followmode]"/>
                            </div>
                            <g:if test="${authChecks[AuthConstants.ACTION_READ]}">
                                <div class="tab-pane" id="schedExDetails${scheduledExecution?.id}">
                                    <div class="presentation" >
                                        <g:render template="execDetails"
                                                  model="[execdata: execution, showArgString: false, hideAdhoc: isAdhoc]"/>
                                    </div>
                                </div>
                            </g:if>
                        </div>
                      </div>
                    </div>
                  </div>
          <g:if test="${scheduledExecution}">
              <div class="col-sm-4">
                  <div class="executionshow_wrap execution_ko">
                      <div class="executionshow">
                          <div class="row">
                              %{--job or adhoc title--}%
                              <div class="col-sm-12">
                                  <div class="card">

                                      <div class="card-footer">
                                          <g:render template="wfstateSummaryDisplay" bean="${workflowState}" var="workflowState"/>

                                      </div>
                                  </div>
                              </div>
                              %{--permalink--}%

                          </div> <!-- end div -->

                      </div>
                  </div>

                  <div class="row" id="activity_section">
                      <div class="col-sm-12">

                          <div class="card card-plain">
                              <div class="card-header">
                                  <h3 class="card-title">
                                      <g:message code="page.section.Activity.for.this.job"/>
                                  </h3>
                              </div>
                          </div>

                          <div class="card">
                              <div class="card-content">
                                  <g:render template="/reports/activityLinks" model="[hideNowRunning:!execution.dateCompleted, execution:execution, scheduledExecution: scheduledExecution, knockoutBinding: true]"/>
                              </div>
                          </div>
                      </div>
                  </div>

              </div>
              </div>
          </g:if>
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
        <g:icon name="menu-right" css="text-primary"/>

    </span>
    <span data-bind="template: { name: 'step-info-simple', data:stepinfo, as: 'stepinfo' }"></span>
  </script>
  <script type="text/html" id="step-info-path-links">
    %{-- Display the full step path with icon and identity --}%
    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path-links', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-primary"/>

    </span>
    <span data-bind="template: { name: 'step-info-simple-link', data:stepinfo, as: 'stepinfo' }"></span>
  </script>
  <script type="text/html" id="step-info-parent-path">
    %{-- Display the full step path with icon and identity --}%

    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-primary"/>
    </span>
  </script>
  <script type="text/html" id="step-info-parent-path-links">
    %{-- Display the full step path with icon and identity --}%

    <span data-bind="if: stepinfo.hasParent()">
        <span data-bind="with: stepinfo.parentStepInfo()">
            <span data-bind="template: { name: 'step-info-path-links', data:$data, as: 'stepinfo' }"></span>
        </span>
        <g:icon name="menu-right" css="text-primary"/>
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

  <!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
  <g:javascript>
    var workflow=null;
    var followControl=null;
    var flowState=null;
    var nodeflowvm=null;
    function followOutput(){
        followControl.beginFollowingOutput('${enc(js:execution?.id)}');
    }
    function followState(){
        try{
            flowState.beginFollowing();
        }catch(e){
            nodeflowvm.errorMessage('Could not load flow state: '+e);
            nodeflowvm.stateLoaded(false);
        }
    }
    function showTab(id){
        jQuery('#'+id+' a').tab('show');
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
        viewoptionsCompleteId:'viewoptionscomplete',
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
        totalCount: '${enc(js:scheduledExecution?.getExecCountStats()?: -1)}'
      });
      nodeflowvm=new NodeFlowViewModel(
        workflow,
        "${enc(js:g.createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id,params:[format:'json']))}",
        "${enc(js:g.createLink(controller: 'execution', action: 'ajaxExecNodeState', id: execution.id))}",
        multiworkflow,
        {followControl:followControl,executionId:'${enc(js:execution.id)}'}
      );
      flowState = new FlowState('${enc(js:execution?.id)}','flowstate',{
        workflow:workflow,
        loadUrl: "${enc(js:g.createLink(controller: 'execution', action: 'ajaxExecState', id: execution.id))}",
        outputUrl:"${g.enc(js:createLink(controller: 'execution', action: 'tailExecutionOutput', id: execution.id,params:[format:'json']))}",
        selectedOutputStatusId:'selectedoutputview',
        reloadInterval:1500
     });
      nodeflowvm.followFlowState(flowState,true);

        ko.mapping.fromJS({
            completed:'${execution.dateCompleted!=null}',
            startTime:'${enc(js:execution.dateStarted)}',
            endTime:'${enc(js:execution.dateCompleted)}',
            executionState:'${enc(js:execution.executionState)}',
            executionStatusString:'${enc(js:execution.status)}'
        },{},nodeflowvm);
        jQuery('.execution_ko').each(function(i,e){
            ko.applyBindings(nodeflowvm,e);
        })
        nodeflowvm.selectedNodes.subscribe(function (newValue) {
            if (newValue) {
                flowState.loadUrlParams=jQuery.extend(flowState.loadUrlParamsBase,{nodes:newValue.join(",")});
            }else{
                flowState.loadUrlParams=flowState.loadUrlParamsBase;
            }
        });
        //link flow and output tabs to initialize following
        //by default show state
        followState();
        jQuery('#tab_link_summary').on('show.bs.tab',function(e){
            nodeflowvm.activeTab("summary");
            followState();
        });
        jQuery('#tab_link_flow').on('show.bs.tab',function(e){
            nodeflowvm.activeTab("flow");
            followState();
        });
        jQuery('#tab_link_output').on('show.bs.tab',function(e){
            nodeflowvm.activeTab("output");
            followOutput();
        });
        jQuery('.toggle-card-collapse').on('click', function(e){
          e.preventDefault()
          var card = jQuery(e.target).data().card;
          jQuery('#' + card).toggle()
        })
        jQuery('.toggle-card-collapse i').on('click', function(e){
          // same function as above, just for the icon, and climbing the parent to get the card
          e.preventDefault()
          var card = jQuery(e.target).parent().data().card;
          jQuery('#' + card).toggle()
        })
        if(document.getElementById('activity_section')){
            activity = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
            activity.nowRunningEnabled(${null != execution?.dateCompleted});
            //enable now running activity tab once execution completes
            activity.highlightExecutionId("${execution.id}");
            nodeflowvm.completed.subscribe(activity.nowRunningEnabled);
            ko.applyBindings(activity, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', activity);
       }
        jQuery('.apply_ace').each(function () {
            _applyAce(this);
        });
        followControl.bindActions('outputappendform');

        PageActionHandlers.registerHandler('copy_other_project',function(el){
            jQuery('#jobid').val(el.data('jobId'));
            jQuery('#selectProject').modal();
        });
        var outDetails = window.location.hash;
        if(outDetails === '#output'){
            nodeflowvm.activeTab("output");
            followOutput();
            showTab('tab_link_output');
        }else if(outDetails === '#monitor'){
            nodeflowvm.activeTab("flow");
            showTab('tab_link_flow');
        }else if(outDetails === '#definition'){
            showTab('tab_link_definition');
        }
        jQuery('.running_link').click()
    }
    jQuery(init);
  </g:javascript>
  </body>
</html>

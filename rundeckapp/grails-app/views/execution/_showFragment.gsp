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

<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:if test="${!execution.dateCompleted}">
    <g:jsonToken id="exec_cancel_token" url="${request.forwardURI}"/>
</g:if>

<g:if test="${inlineView}">
  <div class="card-header">
    <div class="executionshow panel-heading-affix" data-affix="top" data-affix-padding-top="8" id="jobInfo_${execution.id}" >
      <div class="row">
        <div class="col-xs-12 col-sm-2">
          <div>
            <a class="has_tooltip arrow-button" data-placement="bottom" title="<g:message code="scroll.to.top" />" href="#top">
              <i class="fas fa-chevron-circle-up fa-2x"></i>
            </a>
            <a class="has_tooltip arrow-button" data-placement="bottom" title="<g:message code="scroll.to.bottom" />" href="#bottom">
              <i class="fas fa-chevron-circle-down fa-2x"></i>
            </a>
          </div>
        </div>
        <div class="col-sm-8 text-center">
          <span class="inline_only">
            <g:link class="primary" title="Show execution #${execution.id}" controller="execution" action="show" id="${execution.id}" params="[project:execution.project]">
              <i class="exec-status icon ${!execution.dateCompleted ? 'running' : execution.status == 'true' ? 'succeed' : execution.cancelled ? 'warn' : 'fail'}"></i>
              <g:message code="execution.identity" args="[execution.id]"/>
            </g:link>
          </span>
          <g:if test="${execution.status == 'scheduled' && execution.dateCompleted == null}">
            <span class="label label-success succeed">
              Scheduled
            </span>
          </g:if>
          <g:elseif test="${null != execution.dateCompleted}">
            <span class="label label-${execution.status == 'true' ? 'success' : 'danger'}">
              <g:if test="${execution.status == 'true'}">
                Succeeded
              </g:if>
              <g:elseif test="${execution.cancelled}">
                Killed
              </g:elseif>
              <g:else>
                Failed
              </g:else>
            </span>
          </g:elseif>
          <g:else>
            <span>
              <!-- ko if: executionState()=='RUNNING' -->
              <g:img class="loading-spinner" file="spinner-gray.gif" width="12px" height="12px"/>
              <!-- /ko -->
              <span class=" execstate execstatedisplay overall" data-execstate="${enc(attr:execState)}" data-bind="attr: { 'data-execstate': executionState(), 'data-statusstring': executionStatusString() } ">
              </span>
            </span>
            <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
              <span data-bind="if: canKillExec()" style="margin-left: 10px">
                <span data-bind="visible: !completed() ">
                  <!-- ko if: !killRequested() || killStatusFailed() -->
                  <span class="btn btn-danger btn-xs" data-bind="click: killExecAction">
                    <g:message code="button.action.kill.job" />
                    <i class="glyphicon glyphicon-remove"></i>
                  </span>
                  <!-- /ko -->
                  <!-- ko if: killRequested() -->
                  <!-- ko if: killStatusPending() -->
                  <g:img class="loading-spinner" file="spinner-gray.gif" width="16px" height="16px"/>
                  <!-- /ko -->
                  <span class="loading" data-bind="text: killStatusText"></span>
                  <!-- /ko -->
                </span>
              </span>
            </g:if>
          </g:else>
      </div>
      <div class="col-sm-2">
          <a class="close closeoutput">
            <i class="fas fa-times-circle" style="font-size:28px;"></i>
          </a>
      </div>
    </div>
    <div class="row" style="margin-top:20px;">
      <div class="col-xs-12 col-sm-10">
        <g:render template="wfItemView" model="[
                  item: execution.workflow.commands[0],
                  icon: 'icon-med',
                  iwidth: '24px',
                  iheight: '24px',
          ]"/>
      </div>
      <div class="col-xs-12 col-sm-2 text-right">
        <span class=" " data-bind="visible: completed() ">
          <g:if test="${scheduledExecution}">
            <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
              <g:link controller="scheduledExecution"
                        action="execute"
                        id="${scheduledExecution.extid}"
                        params="${[retryExecId: execution.id,project:execution.project]}"
                        class="btn btn-default btn-xs"
                        title="${g.message(code: 'execution.job.action.runAgain')}">
                <i class="glyphicon glyphicon-play"></i>
                <g:message code="execution.action.runAgain"/>&hellip;
              </g:link>
            </g:if>
          </g:if>
          <g:else>
            <g:if test="${jobCreateAllowed}">
              <g:link
                      controller="scheduledExecution"
                      action="createFromExecution"
                      params="${[executionId: execution.id, project: execution.project]}"
                      class="btn btn-default btn-xs"
                      title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}">
                  <g:message code="execution.action.saveAsJob" default="Save as Job"/>
              </g:link>
            </g:if>
          </g:else>
        </span>
      </div>
    </div>
  </div>
</div>
</g:if>
<div class="${inlineView?'card-content inlineexecution':''}">
  <div id="commandFlow" class="outputcontrols" style="margin-bottom:1em;">
    <form action="#" id="outputappendform">
      <div class="row" style="border-bottom:1px solid #e8e8e8;">
        <div class="col-sm-8" style="margin-bottom: 10px">
          <a href="#" class="collapser btn btn-xs" data-toggle="collapse" data-target="#viewoptions" title="Log Output View Options">
            View Options
          </a>
          <div class="collapse" id="viewoptions" style="padding-top:1em;">
            <div style="${wdgt.styleVisible(unless: followmode == 'node')}" class="obs_node_false ">
              <span class="obs_grouped_false" style="${wdgt.styleVisible(if: followmode == 'tail')}">
                <span class="text-primary">Log view:</span>
                <label class="action  join" title="Click to change" id="colTimeShowLabel">
                  <g:checkBox name="coltime" id="colTimeShow" value="true" checked="true" class="opt_display_col_time"/>
                  Time
                </label>
                <label class="action  join" title="Click to change" id="colNodeShowLabel">
                  <g:checkBox name="coltime id="colNodeShow value="true checked="true class="opt_display_col_node"/>
                  Node
                </label>
                <label class="action" title="Click to change" id="colStepShowLabel">
                  <g:checkBox name="coltime" id="colStepShow" value="true" checked="${!inlineView}" class="opt_display_col_step"/>
                  Step
                </label>
               </span>
            </div>
            <div>
              <span class="text-primary">Node view:</span>
              <label class="out_setmode_toggle out_setmode">
                  <input type="checkbox" ${followmode == 'node' ? 'checked' : ''}/>
                  <g:message code="execution.show.mode.Compact.title" default="Compact"/>
              </label>
              <label class="ansi-color-toggle">
                  <input type="checkbox" checked/>
                  <g:message code="execution.show.mode.ansicolor.title" default="Ansi Color"/>
              </label>
              <label class="log-wrap-toggle">
                  <input type="checkbox" checked/>
                  <g:message code="execution.show.mode.wrapmode.title" default="Wrap Long Lines"/>
              </label>
            </div>
          </div>
        </div>
        <div class="col-sm-4">
          <div class="pull-right">
            <span class="tabs-sibling" style="${execution.dateCompleted ? '' : 'display:none'}" id="viewoptionscomplete">
              <span>
                <g:link class="btn btn-default btn-xs"
                        title="${message(code:'execution.show.log.text.button.description',default:'View text output')}"
                        controller="execution" action="downloadOutput" id="${execution.id}"
                        params="[view: 'inline', formatted: false, project: execution.project,
                                stripansi:true]">
                  <g:message code="execution.show.log.text.button.title" />
                </g:link>
              </span>
              <span>
                <g:link class="btn btn-default btn-xs"
                        title="${message(code:'execution.show.log.html.button.description',default:'View rendered output')}"
                        controller="execution" action="renderOutput" id="${execution.id}"
                        params="[project: execution.project, ansicolor:'on',loglevels:'on',convertContent:'on']">
                  <g:message code="execution.show.log.html.button.title" />
                </g:link>
              </span>
              <span class="sepL">
                <g:link class="btn btn-default btn-xs"
                      title="${message(code:'execution.show.log.download.button.description',default:'Download {0} bytes',args:[filesize>0?filesize:'?'])}"
                      controller="execution" action="downloadOutput" id="${execution.id}"
                      params="[project: execution.project]">
                  <b class="glyphicon glyphicon-file"></b>
                  <g:message code="execution.show.log.download.button.title" />
                </g:link>
              </span>
            </span>
          </div>
        </div>
      </div>
    </form>
  </div>
  <div id="fileload" style="display:none; border-bottom:1px solid #b7b7b7;" class=" row">
    <div class="col-sm-12">
      <div class=" progress " id="fileloadprogress" style="width:100%">
        <div class="progress-bar progress-bar-info" role="progressbar" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style="width: 10%;">
          <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure-waiting.gif')}" alt="Spinner"/>
            <span id="fileloadpercent"></span>
        </div>
      </div>
    </div>
  </div>
  <div id="commandPerform" class="commandcontent ansicolor ansicolor-on" style="display:none; overflow-x: auto;"></div>
  <div id="log"></div>
  <div id="commandPerform_empty" style="display: none">
    <div class="row">
      <div class="col-sm-12">
        <div class="well well-nobg inline">
          <p class="text-primary">
            <i class="glyphicon glyphicon-info-sign"></i>
            <em><g:message code="execution.log.no.output"/></em>
          </p>
        </div>
      </div>
    </div>
  </div>
  <div id="commandPerform_clusterinfo" style="display: none">
    <div class="row">
      <div class="col-sm-12">
        <div class="well well-nobg inline">
          <p class="text-primary">
            <i class="glyphicon glyphicon-info-sign"></i>
            <em><g:message code="execution.log.clusterExec.log.delayed.message" /></em>
          </p>
        </div>
      </div>
    </div>
  </div>
</div>

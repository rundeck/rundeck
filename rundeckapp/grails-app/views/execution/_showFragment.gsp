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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>

<g:if test="${!execution.dateCompleted}">
    <g:jsonToken id="exec_cancel_token" url="${request.forwardURI}"/>
</g:if>

<g:if test="${inlineView}">
  <div class="card-header">
    <div class="executionshow panel-heading-affix" data-affix="top" data-affix-padding-top="8" id="jobInfo_${execution.id}" >

      <div class="row">
        <div class="col-xs-12 col-sm-6">
          <i class="exec-status icon "
             data-bind="attr: { 'data-execstate': executionState, 'data-statusstring':executionStatusString }">
          </i>
          <g:render template="wfItemView" model="[item: execution.workflow.commands[0], icon: 'icon-small']"/>
      </div>

        <div class="col-xs-12 col-sm-6 text-right execution-action-links">
          <g:if test="${scheduledExecution}">
            <g:if test="${authChecks[AuthConstants.ACTION_RUN]}">
              <g:link controller="scheduledExecution"
                      action="execute"
                      id="${scheduledExecution.extid}"
                      params="${[retryExecId: execution.id, project: execution.project]}"
                      class="btn btn-default btn-xs"
                      data-bind="visible: completed() "
                      title="${g.message(code: 'execution.job.action.runAgain')}">
                <i class="glyphicon glyphicon-play"></i>
                <g:message code="execution.action.runAgain"/>
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
                      data-bind="visible: completed() "
                      title="${g.message(code: 'execution.action.saveAsJob', default: 'Save as Job')}">
                <g:message code="execution.action.saveAsJob" default="Save as Job"/>
              </g:link>
            </g:if>
          </g:else>


          <g:if test="${authChecks[AuthConstants.ACTION_KILL]}">
            <!-- ko if: canKillExec() -->

              <span data-bind="visible: !completed() " class="spacer">
                <!-- ko if: !killRequested() || killStatusFailed() -->
                <span class="btn btn-danger btn-xs" data-bind="click: killExecAction">
                  <g:message code="button.action.kill.job"/>
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
            <!-- /ko -->
          </g:if>

          <g:render template="/scheduledExecution/showExecutionLink"
                    model="[execution: execution, hideExecStatus: true]"/>

          <span class="spacer">
            <!-- ko if: executionState()=='RUNNING' -->
            <g:img class="loading-spinner" file="spinner-gray.gif" width="12px" height="12px"/>
            <!-- /ko -->
            <span class=" execstate execstatedisplay overall" data-execstate="${enc(attr: execState)}"
                  data-bind="attr: { 'data-execstate': executionState(), 'data-statusstring': executionStatusString() } ">
            </span>
          </span>


          <a class="close closeoutput " data-bind="bootstrapTooltip: true" title="Close output">
            <i class="fas fa-times-circle"></i>
          </a>
      </div>
    </div>
  </div>
</div>

  <div data-bind="visible: logoutput().fileLoadError" class="executionshow alert alert-warning" style="display: none">
    <span data-bind="text: logoutput().fileLoadError" ></span>
  </div>
  <div class="execution-show-log"></div>

</g:if>
<div class="executionshow ${inlineView ? 'card-content tight inlineexecution' : ''}">
  <div data-bind="if: logoutput().loadingFile">
    <div class="card-content-full-width">
      <div class=" progress progress-embed progress-square flex-container flex-justify-start" >
        <div class="progress-bar progress-bar-info flex-basis-auto "
             role="progressbar"
             aria-valuenow="10"
             aria-valuemin="0"
             aria-valuemax="100"
             style="width: 30%;"
          data-bind="style: {width: logoutput().fileLoadPercentWidth}, attr: {'aria-valuenow': logoutput().fileLoadPercentage }">
            <i class="fas fa-spinner fa-pulse" data-bind="visible: logoutput().running()"></i>
            <span data-bind="text: logoutput().fileLoadText"></span>

        </div>
        <span class="flex-item-auto"></span>

          <span class="btn-xs btn btn-simple btn-default pull-right" data-bind="click: logoutput().pauseLoading, if: logoutput().running() && !logoutput().paused()">
            Stop Loading
            <i class="glyphicon glyphicon-pause"></i>
          </span>
          <span class="btn-xs btn btn-simple btn-default pull-right" data-bind="click: logoutput().resumeLoading, if: !logoutput().running() && logoutput().paused()">
            Resume Loading
            <i class="glyphicon glyphicon-download-alt"></i>
          </span>

      </div>
    </div>
  </div>
  <div data-bind="if: logoutput().running() && !logoutput().loadingFile() && !logoutput().fileLoadError()">
    <div class="card-content-full-width">
      <div class=" progress progress-embed progress-square progress-striped active" style="height: 1.5em">
        <div class="progress-bar progress-bar-default "
             role="progressbar"
             aria-valuenow="0"
             aria-valuemin="0"
             aria-valuemax="100"
             style="width: 100%;">
            <i class="fas fa-spinner fa-pulse"></i>
            Loading
        </div>
      </div>
    </div>
  </div>

  <div id="commandPerform" class="card-content-full-width ansicolor ansicolor-on"
    data-bind="css: {'-view-opt--node-inset-disabled': !logoutput().options.showNodeInset() }"
       style="display:none; overflow-x: auto;"></div>
  <div id="log"></div>
  <div id="commandPerform_empty" style="display: none">
    <div class="row">
      <div class="col-sm-12">
        <div class="well well-nobg inline">
          <p class="text-strong">
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
          <p class="text-strong">
            <i class="glyphicon glyphicon-info-sign"></i>
            <em><g:message code="execution.log.clusterExec.log.delayed.message" /></em>
          </p>
        </div>
      </div>
    </div>
  </div>
</div>

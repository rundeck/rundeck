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

<%@ page import="org.rundeck.core.auth.AuthConstants; rundeck.ScheduledExecution" %>

<div class="jobInfoSection">
    <g:if test="${scheduledExecution.groupPath}">
        <section class="text-secondary">
            <g:render template="/scheduledExecution/groupBreadcrumbs" model="[groupPath:scheduledExecution.groupPath,project:scheduledExecution.project, linkCss:'text-secondary']"/>
        </section>
    </g:if>
  <section class="${scheduledExecution.groupPath?'section-space':''}" id="jobInfo_">
    <g:set var="authProjectExport" value="${auth.resourceAllowedTest(
            context: 'application',
            type: 'project',
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT],
            any: true,
            name: scheduledExecution.project
    )}"/>
    <g:set var="authProjectImport" value="${auth.resourceAllowedTest(
            context: 'application',
            type: 'project',
            action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT],
            any: true,
            name: scheduledExecution.project
    )}"/>
    <g:set var="exportStatus" value="${authProjectExport && scmExportEnabled ? scmExportStatus?.get(scheduledExecution.extid) :null}"/>
    <g:set var="importStatus" value="${authProjectImport && scmImportEnabled ? scmImportStatus?.get(scheduledExecution.extid):null}"/>

      <span class="${linkCss ?: 'card-title h3'}">
      <g:if test="${includeExecStatus}">
          <b class="exec-status icon "
             data-bind="attr: { 'data-execstate': executionState, 'data-statusstring':executionStatusString }">
          </b>
      </g:if>
        <g:link controller="scheduledExecution" action="${jobAction?:'show'}"
            class="text-primary"
            params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <g:enc>${scheduledExecution?.jobName}</g:enc>
        </g:link>
        <g:if test="${jobActionButtons}">
            <div class="job-action-button">
                <g:render template="/scheduledExecution/jobActionButton"
                          model="[scheduledExecution: scheduledExecution, hideTitle: true, btnClass: 'btn btn-secondary btn-sm']"/>
          </div>
        </g:if>
      </span>
      <g:render template="/scm/statusBadge"
                model="[
                        showClean:true,
                        linkClean:true,
                        exportStatus: exportStatus?.synchState?.toString(),
                        importStatus: importStatus?.synchState?.toString(),
                        text  : '',
                        notext: false,
                        link: true,
                        integration:'export',
                        job:scheduledExecution,
                        exportCommit  : exportStatus?.commit,
                        importCommit  : importStatus?.commit,
                ]"/>

      <g:if test="${ !scheduledExecution.hasExecutionEnabled()}">
          <span class=" label label-warning has_tooltip" data-toggle="tooltip"
                data-placement="auto bottom" title="${message(code:'scheduleExecution.execution.disabled')}">
              <i class="glyphicon ${scheduledExecution.scheduled?'glyphicon-time':'glyphicon-ban-circle'}"></i>
              <span class="detail"><g:message code="disabled" /></span>
          </span>
      </g:if>
      <g:if test="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions) && nextExecution}">
          <span class="scheduletime">
              <g:if test="${serverNodeUUID && !remoteClusterNodeUUID}">
                  <span class="text-warning has_tooltip" title="${message(code:"scheduledExecution.scheduled.cluster.orphan.title")}"
                        data-placement="right"
                  >
                      <g:icon name="alert"/>
                  </span>
              </g:if>
              <g:else>
                  <g:icon name="time"/>
              </g:else>
              <g:set var="titleHint"
                     value="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0") : ''}"/>
              <span title="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0", args:[remoteClusterNodeUUID]) : ''} at ${enc(attr:g.relativeDate(atDate:nextExecution))}">
                  <g:relativeDate elapsed="${nextExecution}"
                                  untilClass="timeuntil"/>
              </span>
              <g:if test="${remoteClusterNodeUUID}">
                  on
                  <span data-server-uuid="${remoteClusterNodeUUID}" data-server-name="${remoteClusterNodeUUID}"
                        data-name-truncated="8"
                        data-uuid-label-none="true"
                        class="rundeck-server-uuid text-secondary">
                      <i class="fas fa-dot-circle text-muted cluster-status-icon"></i>
                  </span>
              </g:if>
          </span>
      </g:if>
      <g:elseif test="${scheduledExecution.scheduled && !g.executionMode(is:'active',project:scheduledExecution.project)}">
          <span class="label label-secondary has_tooltip" data-toggle="tooltip"
              data-placement="auto bottom"
                title="${g.message(code: 'disabled.schedule.run')}">
              <i class="glyphicon glyphicon-time"></i>
              <span class="detail"><g:message code="disabled.schedule.run" /></span>
          </span>
      </g:elseif>
      <g:elseif test="${scheduledExecution.scheduled && !scheduledExecution.hasScheduleEnabled()}">
          <span class=" label label-muted has_tooltip" data-toggle="tooltip"
                data-placement="auto bottom" title="${message(code:'scheduleExecution.schedule.disabled')}">
              <i class="glyphicon glyphicon-time"></i>
              <span class="detail"><g:message code="disabled" /></span>
          </span>
      </g:elseif>
      <g:elseif test="${scheduledExecution.scheduled && scheduledExecution.shouldScheduleExecution() && !nextExecution}">
          <span class="label label-warning  has_tooltip" data-toggle="tooltip"
              data-placement="auto bottom"
                title="${g.message(code: 'job.schedule.will.never.fire')}">
              <i class="glyphicon glyphicon-time"></i>
              <span class="detail"><g:message code="never" /></span>
          </span>
      </g:elseif>

  </section>

    <section class="section-space">
        <g:render template="/scheduledExecution/description"
                  model="[
                          description : scheduledExecution.description,
                          textCss     : 'h5 text-primary',
                          mode        : jobDescriptionMode ?: 'expanded',
                          cutoffMarker: ScheduledExecution.RUNBOOK_MARKER,
                          jobLinkId   : scheduledExecution.extid,
                          rkey        : g.rkey()
                  ]"/>
</section>
</div>

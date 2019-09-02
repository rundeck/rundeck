%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
<g:set var="authUpdate" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_UPDATE])}"/>
<g:set var="authRead" value="${auth.jobAllowedTest(job: scheduledExecution, any: true, action: [AuthConstants.ACTION_READ])}"/>
<g:set var="authDelete" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_DELETE])}"/>
<g:set var="authEnableDisableSchedule" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_TOGGLE_SCHEDULE])}"/>
<g:set var="authEnableDisableExecution" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_TOGGLE_EXECUTION])}"/>
<g:set var="authJobCreate" value="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE, project: scheduledExecution.project)}"/>
<g:set var="authOtherProject" value="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE, project: scheduledExecution.project, others: true)}"/>
<g:set var="authJobDelete" value="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_DELETE, project: scheduledExecution.project)}"/>
<g:set var="authProjectExport" value="${auth.resourceAllowedTest(
        context: 'application',
        type: 'project',
        action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT],
        any: true,
        name: scheduledExecution.project
)}"/>
<g:set var="renderedActions" value="${0}"/>
<g:if test="${authUpdate}">
    %{renderedActions++}%
    <li>
        <g:link controller="scheduledExecution"
                title="${g.message(code:'scheduledExecution.action.edit.button.tooltip')}"
                action="edit"
                params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}" class="">
            <i class="glyphicon glyphicon-edit"></i>
            <g:message code="scheduledExecution.action.edit.button.label"/>
        </g:link>
    </li>
</g:if>
<g:if test="${authRead && authJobCreate}">
    %{renderedActions++}%
    <li>
        <g:link controller="scheduledExecution"
                title="${g.message(code:'scheduledExecution.action.duplicate.button.tooltip')}"
                action="copy"
                params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}" class="">
            <i class="glyphicon glyphicon-plus"></i>
            <g:message
                    code="scheduledExecution.action.duplicate.button.label"/>
        </g:link>
    </li>
</g:if>
<g:if test="${authRead && authOtherProject}">
    %{renderedActions++}%
    <li>
        <g:link controller="scheduledExecution"
                title="${g.message(code:'scheduledExecution.action.duplicate.button.tooltip')}"
                action="copyanother"
                data-job-id="${enc(attr: scheduledExecution.extid)}"
                data-action="copy_other_project"
                class="page_action">
            <i class="glyphicon glyphicon-plus"></i>
            <g:message
                    code="scheduledExecution.action.duplicate.other.button.label"/>
        </g:link>

    </li>
</g:if>

<g:unless test="${hideJobDelete}">
    <g:if test="${authJobDelete && authDelete}">
        <g:if test="${authUpdate || authRead&&authJobCreate}">
            <li class="divider"></li>
        </g:if>
        %{renderedActions++}%
        <li>
            <g:link
                controller="scheduledExecution"
                action="delete"
                params="${[id:scheduledExecution.extid,project: scheduledExecution.project]}"
                class="page_action"
                data-action="job_delete_single"
                data-job-id="${enc(attr: scheduledExecution.extid)}"
               title="${g.message(code: 'delete.this.job')}">
                <b class="glyphicon glyphicon-remove-circle"></b>
                <g:message code="scheduledExecution.action.delete.button.label"/>
            </g:link>
        </li>
    </g:if>
</g:unless>

<g:if test="${authEnableDisableSchedule && scheduledExecution.scheduled || authEnableDisableExecution}">
    <li class="divider"></li>
</g:if>
<g:if test="${authEnableDisableSchedule && scheduledExecution.scheduled}">
    <li>
        %{renderedActions++}%
        <g:if test="${scheduledExecution.hasScheduleEnabled()}">
            <g:link controller="scheduledExecution"
                    action="flipScheduleEnabled"
                    params="${[id:scheduledExecution.extid,project: scheduledExecution.project, scheduleEnabled: false]}"
                    data-job-id="${enc(attr: scheduledExecution.extid)}"
                    class="page_action"
                    data-action="disable_job_schedule_single"
                    title="${g.message(code: 'disable.schedule.this.job')}">
                <b class="glyphicon glyphicon-unchecked"></b>
                <g:message code="scheduledExecution.action.disable.schedule.button.label"/>
            </g:link>
        </g:if>
        <g:else>
            <g:link controller="scheduledExecution"
                    action="flipScheduleEnabled"
                    params="${[id:scheduledExecution.extid,project: scheduledExecution.project, scheduleEnabled: true]}"
                    data-job-id="${enc(attr: scheduledExecution.extid)}"
                    class="page_action"
                    data-action="enable_job_schedule_single"
                    title="${g.message(code: 'enable.schedule.this.job')}">
                <b class="glyphicon glyphicon-check"></b>
                <g:message code="scheduledExecution.action.enable.schedule.button.label"/>
            </g:link>
        </g:else>
    </li>
</g:if>

<g:if test="${authEnableDisableExecution}">
    %{renderedActions++}%
    <li>
        <g:if test="${scheduledExecution.hasExecutionEnabled()}">
            <g:link controller="scheduledExecution"
                    action="flipExecutionEnabled"
                    params="${[id:scheduledExecution.extid,project: scheduledExecution.project, executionEnabled: false]}"
                    data-job-id="${enc(attr: scheduledExecution.extid)}"
                    class="page_action"
                    data-action="disable_job_execution_single"
                    title="${g.message(code: 'disable.execution.this.job')}">
                <b class="glyphicon glyphicon-unchecked"></b>
                <g:message code="scheduledExecution.action.disable.execution.button.label"/>
            </g:link>
        </g:if>
        <g:else>
            <g:link controller="scheduledExecution"
                    action="flipExecutionEnabled"
                    params="${[id:scheduledExecution.extid,project: scheduledExecution.project, executionEnabled: true]}"
                    data-job-id="${enc(attr: scheduledExecution.extid)}"
                    class="page_action"
                    data-action="enable_job_execution_single"
                    title="${g.message(code: 'enable.execution.this.job')}">
                <b class="glyphicon glyphicon-check"></b>
                <g:message code="scheduledExecution.action.enable.execution.button.label"/>
            </g:link>
        </g:else>
    </li>
</g:if>

<g:if test="${authRead}">
    %{renderedActions++}%
    <g:if test="${authJobDelete && authDelete || authUpdate || authJobCreate}">
        <li class="divider"></li>
    </g:if>
    <li><g:link controller="scheduledExecution"
                title="${g.message(code: 'scheduledExecution.action.downloadformat.button.label', args: ['XML'])}"
                params="[project: scheduledExecution.project,format:'xml']"
                action="show"
                id="${scheduledExecution.extid}">
        <b class="glyphicon glyphicon-file"></b>
        <g:message code="scheduledExecution.action.downloadformat.button.label"
                   args="['XML']"/>
    </g:link>
    </li>
    <li>
        <g:link controller="scheduledExecution"
                title="${g.message(code: 'scheduledExecution.action.downloadformat.button.label', args: ['YAML'])}"
                params="[project: scheduledExecution.project,format:'yaml']"
                action="show"
                id="${scheduledExecution.extid}">
            <b class="glyphicon glyphicon-file"></b>
            <g:message
                    code="scheduledExecution.action.downloadformat.button.label"
                    args="['YAML']"/>
        </g:link>
    </li>
</g:if>

<g:if test="${scmExportEnabled && scmExportStatus?.get(scheduledExecution.extid)}">
    %{renderedActions++}%
    <g:if test="${authRead}">
        <li class="divider"></li>
    </g:if>

    <li class="dropdown-header"><g:message code="scm.export.plugin" /></li>

    <g:set var="jobstatus" value="${scmExportStatus?.get(scheduledExecution.extid)}"/>
    <g:set var="exportStateClean" value="${jobstatus?.synchState?.toString()=='CLEAN'}"/>
    <g:set var="exportStateCreate" value="${'CREATE_NEEDED'==jobstatus?.synchState?.toString()}"/>
    <g:each in="${jobstatus?.actions}" var="action">
        <g:if test="${action.id == '-'}">
            <li class="divider"></li>
        </g:if>
        <g:else>
            <li>
                <g:render template="/scm/actionLink"
                          model="[action:action,
                                  integration:'export',
                                  project:params.project,
                                  linkparams:[id: scheduledExecution.extid]]"
                />

            </li>
        </g:else>
    </g:each>
    <g:unless test="${exportStateCreate}">
        <li><g:link controller="scm"
                    params="[project: scheduledExecution.project,id:scheduledExecution.extid,integration: 'export']"
                    action="diff"
                    >
            <g:render template="/scm/statusBadge"
                      model="[exportStatus: jobstatus?.synchState?.toString(),
                              importStatus: null,
                              text  : '',
                              notext: true,
                              integration: 'export',
                              icon:'glyphicon-eye-open',
                              exportCommit  : jobstatus?.commit]"/>
            <g:if test="${exportStateClean}">
                <g:message code="scm.action.diff.clean.button.label" default="View Commit Info"/>
            </g:if>
            <g:else>
                <g:message code="scm.action.diff.button.label" default="Diff Changes"/>
            </g:else>
        </g:link>
        </li>
    </g:unless>
</g:if>

<g:if test="${scmImportEnabled && scmImportStatus?.get(scheduledExecution.extid)}">
    %{renderedActions++}%


    <g:set var="jobstatus" value="${scmImportStatus?.get(scheduledExecution.extid)}"/>
    <g:set var="importStateClean" value="${jobstatus?.synchState?.toString()=='CLEAN'}"/>

    <g:set var="importStateUnknown" value="${'UNKNOWN'==jobstatus?.synchState?.toString()}"/>
        <g:if test="${authRead}">
            <li class="divider"></li>
        </g:if>
        <li class="dropdown-header"><g:message code="scm.import.plugin" /></li>
    <g:each in="${jobstatus?.actions}" var="action">
        <g:if test="${action.id == '-'}">
            <li class="divider"></li>
        </g:if>
        <g:else>
            <li>
                <g:render template="/scm/actionLink"
                          model="[action:action,
                                  integration:'import',
                                  project:params.project,
                                  linkparams:[id: scheduledExecution.extid]]"
                />

            </li>
        </g:else>
    </g:each>
    <g:unless test="${importStateUnknown}">
    <li>
        <g:link controller="scm"
                params="[project: scheduledExecution.project,id:scheduledExecution.extid,integration: 'import']"
                action="diff">
            <g:render template="/scm/statusBadge"
                  model="[importStatus: jobstatus?.synchState?.toString(),
                          text  : '',
                          notext: true,
                          integration: 'import',
                          icon:'glyphicon-eye-open',
                          exportCommit  : jobstatus?.commit]"/>
            <g:if test="${importStateClean}">
                <g:message code="scm.action.diff.clean.button.label" default="View Commit Info"/>
            </g:if>
            <g:else>
                <g:message code="scm.action.diff.button.label" default="Diff Changes"/>
            </g:else>
        </g:link>
    </li>
    </g:unless>
    <g:if test="${importStateUnknown}">
        <li class="dropdown-header">
            <g:render template="/scm/statusBadge"
                      model="[importStatus: jobstatus?.synchState?.toString(),
                              exportStatus:null,
                              text: '',
                              notext: false,
                              integration: 'import',
                              importCommit: jobstatus?.commit]"
            />
        </li>
    </g:if>
</g:if>
<g:if test="${renderedActions<1}">
    <li class="dropdown-header">
        <g:message code="scheduledExecution.action.menu.none-available" />
    </li>
</g:if>

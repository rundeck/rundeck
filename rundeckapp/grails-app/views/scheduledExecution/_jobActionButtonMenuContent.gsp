<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
%{--
  - Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
<g:set var="authUpdate" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_UPDATE])}"/>
<g:set var="authRead" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_READ])}"/>
<g:set var="authDelete" value="${auth.jobAllowedTest(job: scheduledExecution, action: [AuthConstants.ACTION_DELETE])}"/>
<g:set var="authJobCreate" value="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_CREATE, project: scheduledExecution.project)}"/>
<g:set var="authJobDelete" value="${auth.resourceAllowedTest(kind: 'job', action: AuthConstants.ACTION_DELETE, project: scheduledExecution.project)}"/>
<g:set var="authProjectExport" value="${auth.resourceAllowedTest(
        context: 'application',
        type: 'project',
        action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT],
        any: true,
        name: scheduledExecution.project
)}"/>
<g:if test="${authUpdate}">
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
<g:unless test="${hideJobDelete}">
    <g:if test="${authJobDelete && authDelete}">
        <g:if test="${authUpdate || authRead&&authJobCreate}">
            <li class="divider"></li>
        </g:if>
        <li>
            <g:if test="${jobDeleteSingle}">
            <g:link
                controller="scheduledExecution"
                action="delete"
                params="${[id:scheduledExecution.extid,project: scheduledExecution.project]}"
                class="act_job_delete_single"
                data-job-id="${enc(attr: scheduledExecution.extid)}"
               title="${g.message(code: 'delete.this.job')}">
                <b class="glyphicon glyphicon-remove-circle"></b>
                <g:message code="scheduledExecution.action.delete.button.label"/>
            </g:link>
            </g:if>
            <g:else>
            <a data-toggle="modal"
                class="act_job_delete_single"
                data-job-id="${enc(attr: scheduledExecution.extid)}"
               href="#jobdelete"
               title="${g.message(code: 'delete.this.job')}">
                <b class="glyphicon glyphicon-remove-circle"></b>
                <g:message code="scheduledExecution.action.delete.button.label"/>
            </a>
            </g:else>
        </li>
    </g:if>
</g:unless>
<g:if test="${authRead}">
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

<g:if test="${authProjectExport && scmExportEnabled && scmStatus?.get(scheduledExecution.extid)}">
    <g:if test="${authRead}">
        <li class="divider"></li>
    </g:if>
    <g:set var="jobstatus" value="${scmStatus?.get(scheduledExecution.extid)}"/>

    <li class="dropdown-header"> SCM Export Plugin</li>

    <li><g:link controller="scm"
                title="${g.message(code: 'scm.action.export', default: 'Commit changes to SCM')}"
                params="[project: scheduledExecution.project,jobIds:scheduledExecution.extid]"
                action="commit"
                >
        <g:render template="/scm/statusBadge"
                  model="[status: jobstatus?.synchState?.toString(),
                          text  : '',
                          notext: true,
                          icon:'glyphicon-circle-arrow-right',
                          meta  : jobstatus?.stateMeta]"/>
        <g:message code="scm.action.commit.button.label" default="Commit Changes"/>
    </g:link>
    </li>
    <li><g:link controller="scm"
                title="${g.message(code: 'scm.action.export', default: 'Commit changes to SCM')}"
                params="[project: scheduledExecution.project,jobId:scheduledExecution.extid]"
                action="diff"
                >
        <g:render template="/scm/statusBadge"
                  model="[status: jobstatus?.synchState?.toString(),
                          text  : '',
                          notext: true,
                          icon:'glyphicon-eye-open',
                          meta  : jobstatus?.stateMeta]"/>
        <g:message code="scm.action.diff.button.label" default="Diff Changes"/>
    </g:link>
    </li>
</g:if>
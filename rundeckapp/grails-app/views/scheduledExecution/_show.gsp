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

<%@ page import="rundeck.ScheduledExecution; com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.Execution" %>
<div class="row">
    <g:render template="/scheduledExecution/showHead"
              model="[scheduledExecution: scheduledExecution,
                      followparams      : [mode: followmode, lastlines: params.lastlines],
                      jobDescriptionMode:'expanded',
                      jobActionButtons  : true,
                      scmExportEnabled  : scmExportEnabled,
                      scmExportStatus   : scmExportStatus,
                      scmImportEnabled  : scmImportEnabled,
                      scmImportStatus   : scmImportStatus
              ]"/>
</div>
<g:set var="runAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}"/>
<g:set var="runEnabled" value="${g.executionMode(is:'active')}"/>
<g:set var="canRunJob" value="${runAccess && runEnabled}"/>
<g:set var="extendeddesc" value="${g.textRemainingLines(text: scheduledExecution.description)}"/>
<g:set var="rundoctext" value="${extendeddesc?g.textAfterLine(text: extendeddesc, marker:ScheduledExecution.RUNBOOK_MARKER):null}"/>
<div class="row">
    <div class="col-sm-12">
        <ul class="nav nav-tabs" id="jobtabs">
            <g:if test="${canRunJob}">
                <li class="active"><a href="#runjob" data-toggle="tab"><g:message
                        code="scheduledExecution.show.run.tab.name"/></a></li>
            </g:if>
            <g:else>
                <li class="disabled">
                    <a href="#"
                       title="${message(code:!runEnabled?'disabled.job.run':'unauthorized.job.run')}"
                       class="has_tooltip"
                       data-placement="bottom">
                        <g:message code="scheduledExecution.show.run.tab.name"/>
                    </a>
                </li>
            </g:else>
            <li class="${canRunJob ? '' : 'active'}"><a href="#definition"
                                                        data-toggle="tab"><g:message code="definition"/></a></li>
            <g:if test="${rundoctext}">
                <li>
                    <a href="#runbook" data-toggle="tab"><g:message code="runbook" /></a>
                </li>
            </g:if>
        </ul>

        <div class="tab-content">
            <g:if test="${canRunJob}">
                <div class="tab-pane active" id="runjob">
                    <tmpl:execOptionsForm
                            model="${[scheduledExecution: scheduledExecution, crontab: crontab, authorized: authorized]}"
                            hideHead="${true}"
                            hideCancel="${true}"
                            defaultFollow="${true}"/>
                </div>
            </g:if>
            <div id="definition"
                 class="tab-pane panel panel-default panel-tab-content  ${canRunJob ? '' : 'active'}">
                <div class="panel-body">
                    <g:render template="/execution/execDetails" model="[execdata: scheduledExecution, strategyPlugins:strategyPlugins, showEdit: true, hideOptions: true, knockout: true]"/>

                </div>
            </div>
            <g:if test="${rundoctext}">
                <div id="runbook" class="tab-pane panel panel-default panel-tab-content">
                    <div class="panel-body">
                        <div class="markdeep">${rundoctext}</div>
                    </div>
                </div>
            </g:if>
        </div>
    </div>
</div>


<div class="row">
    <div class="col-sm-12 ">
        <h4 class="text-muted"><g:message code="statistics" /></h4>

        <g:render template="/scheduledExecution/renderJobStats" model="${[scheduledExecution: scheduledExecution]}"/>
    </div>
</div>

<div class="row" id="activity_section">
    <div class="col-sm-12">
        <h4 class="text-muted"><g:message code="page.section.Activity.for.this.job" /></h4>

        <g:render template="/reports/activityLinks" model="[scheduledExecution: scheduledExecution, knockoutBinding:true]"/>
    </div>
</div>

<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
<g:javascript>
    fireWhenReady('definition', function (z) {
        jQuery('.apply_ace').each(function () {
            _applyAce(this,'400px');
        });
    });
</g:javascript>

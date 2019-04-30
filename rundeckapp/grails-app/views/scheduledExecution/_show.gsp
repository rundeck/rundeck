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

<g:set var="runAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}"/>
<g:set var="readAccess" value="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_READ)}"/>
<g:set var="runEnabled" value="${g.executionMode(is: 'active', project: scheduledExecution.project)}"/>
<g:set var="canRunJob" value="${runAccess && runEnabled}"/>
<g:set var="extendeddesc" value="${g.textRemainingLines(text: scheduledExecution.description)}"/>
<g:set var="rundoctext"
       value="${extendeddesc ? g.textAfterLine(text: extendeddesc, marker: ScheduledExecution.RUNBOOK_MARKER) : null}"/>

<div class="container-fluid">
  <div class="row">
      <div class="col-xs-8">
      <div class="card">
        <div class="card-content">
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
      </div>

          <div class="card">
              <div class="card-content">
                  <div class="vue-tabs">
                      <div class="nav-tabs-navigation">
                          <div class="nav-tabs-wrapper">
                              <ul class="nav nav-tabs" id="jobtabs">
                                  <g:if test="${canRunJob}">
                                      <li class="active"><a href="#runjob" data-toggle="tab"><g:message
                                              code="scheduledExecution.show.run.tab.name"/></a></li>
                                  </g:if>
                                  <g:else>
                                      <li class="disabled">
                                          <a href="#"
                                             title="${message(
                                                     code: !runEnabled ? 'disabled.job.run' :
                                                           'unauthorized.job.run'
                                             )}"
                                             class="has_tooltip"
                                             data-placement="bottom">
                                              <g:message code="scheduledExecution.show.run.tab.name"/>
                                          </a>
                                      </li>
                                  </g:else>
                                  <g:if test="${readAccess}">
                                      <li class="${canRunJob ? '' : 'active'}"><a href="#definition"
                                                                                  data-toggle="tab"><g:message
                                                  code="definition"/></a></li>
                                  </g:if>
                                  <g:if test="${rundoctext}">
                                      <li class="${(canRunJob || readAccess) ? '' : 'active'}">
                                          <a href="#runbook" data-toggle="tab"><g:message code="runbook"/></a>
                                      </li>
                                  </g:if>
                              </ul>
                          </div>
                      </div>

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
                          <g:if test="${readAccess}">
                              <div id="definition" class="tab-pane ${canRunJob ? '' : 'active'}">
                                  <g:render template="/execution/execDetails"
                                            model="[execdata: scheduledExecution, strategyPlugins: strategyPlugins, showEdit: true, hideOptions: true, knockout: true]"/>
                              </div>
                          </g:if>
                          <g:if test="${rundoctext}">
                              <div id="runbook" class="tab-pane  ${(canRunJob || readAccess) ? '' : 'active'}">
                                  <div class="markdeep">${rundoctext}</div>
                              </div>
                          </g:if>
                      </div>
                      <!-- end tab content -->
                  </div>

              </div>
          </div>
    </div>

      <div class="col-xs-4">
          <div class="row">
              <g:render template="/scheduledExecution/renderJobStats"
                        model="${[scheduledExecution: scheduledExecution]}"/>
          </div>

          <div class="row">
              <div class="card" id="activity_section">
                  <div class="card-content">
                      <g:render template="/reports/activityLinks"
                                model="[scheduledExecution: scheduledExecution, knockoutBinding: true, includeJobRef: (
                                        scheduledExecution.getRefExecCountStats() ? true : false
                                )]"/>
                  </div>
              </div>
          </div>
      </div>
  </div>



</div>

<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
<g:javascript>
    fireWhenReady('definition', function (z) {
        jQuery('.apply_ace').each(function () {
            _applyAce(this,'400px');
        });
    });
</g:javascript>

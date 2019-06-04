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
<g:set var="runEnabled" value="${g.executionMode(is: 'active', project: scheduledExecution.project) && scheduledExecution.hasExecutionEnabled()}"/>
<g:set var="canRunJob" value="${runAccess && runEnabled}"/>
<g:set var="extendeddesc" value="${g.textRemainingLines(text: scheduledExecution.description)}"/>
<g:set var="rundoctext"
       value="${extendeddesc ? g.textAfterLine(text: extendeddesc, marker: ScheduledExecution.RUNBOOK_MARKER) : null}"/>
<content tag="subtitlecss">job-page</content>
<content tag="subtitlesection">

    <div class="  subtitle-head flex-container flex-align-items-stretch">
        <div class="subtitle-head-item  flex-item-auto">
            <g:render template="/scheduledExecution/showHead"
                      model="[scheduledExecution: scheduledExecution,
                              followparams      : [mode: followmode, lastlines: params.lastlines],
                              jobDescriptionMode: 'expanded',
                              jobActionButtons  : false,
                              linkCss           : 'text-h4',
                              scmExportEnabled  : scmExportEnabled,
                              scmExportStatus   : scmExportStatus,
                              scmImportEnabled  : scmImportEnabled,
                              scmImportStatus   : scmImportStatus
                      ]"/>

            <section class="section-space">
                <small class="uuid text-secondary">${scheduledExecution.extid}</small>
            </section>
        </div>

        <div class="subtitle-head-item  flex-container column flex-justify-space-between flex-align-items-flex-end">

            <div class="job-action-button ">
                <g:render template="/scheduledExecution/jobActionButton"
                          model="[scheduledExecution: scheduledExecution,
                                  hideTitle         : false,
                                  dropdownClass     : 'dropdown-menu-right',
                                  btnClass          : 'btn btn-secondary btn-sm']"/>
            </div>

            <g:if test="${readAccess}">
                <section class="section-space">
                    <a href="#job-definition-modal" data-toggle="modal" class="btn btn-secondary btn-sm ">
                        <i class="glyphicon glyphicon-info-sign"></i>
                        <g:message code="definition"/>
                    </a>
                </section>

                <g:render template="/common/modal"
                          model="[modalid   : 'job-definition-modal',
                                  modalsize : 'modal-lg',
                                  title     : scheduledExecution.jobName,
                                  cancelCode: 'close'
                          ]">
                    <div data-ko-bind="jobNodeFilters" id="detailtable">
                        <g:render template="/execution/execDetails"
                                  model="[execdata: scheduledExecution, strategyPlugins: strategyPlugins, showEdit: true, hideOptions: true, knockout: true]"/>
                    </div>
                </g:render>
            </g:if>

        </div>
    </div>
</content>

<div class="container-fluid">
    <div class="row">
        <div class="col-xs-12">
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
                                  <g:if test="${rundoctext}">
                                      <li class="${(canRunJob ) ? '' : 'active'}">
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

                          <g:if test="${rundoctext}">
                              <div id="runbook" class="tab-pane  ${(canRunJob ) ? '' : 'active'}">
                                  <div class="markdeep">${rundoctext}</div>
                              </div>
                          </g:if>
                          <div id="_job_content_placeholder" class="tab-pane"></div>
                      </div>
                      <!-- end tab content -->
                  </div>

              </div>
          </div>
    </div>
    </div>
    <div class="row" id="_job_main_placeholder">
        <div class="col-xs-12">
            <div class="card" id="activity_section">
                <div class="card-content">

                    <div class="vue-tabs">
                        <div class="nav-tabs-navigation">
                            <div class="nav-tabs-wrapper">
                                <ul class="nav nav-tabs activity_links">
                                    <li class="active">
                                        <a href="#stats" data-toggle="tab"><g:message code="job.view.stats.label" /></a>
                                    </li>
                                    <li>
                                        <a href="#history" data-toggle="tab"><g:message code="job.view.history.label" /></a>
                                    </li>
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
                            <div class="tab-pane" id="history">

                                <div data-ko-bind="history" class="_history_content vue-project-activity">

                                    <activity-list :event-bus="EventBus"></activity-list>
                                </div>
                            </div>
                        </div>
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

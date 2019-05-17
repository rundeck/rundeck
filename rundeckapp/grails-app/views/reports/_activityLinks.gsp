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

<g:set var="projParams" value="${[project: project ?: params.project ?: request.project]}"/>
<g:set var="linkParams" value="${filter?filter+projParams:projParams}"/>
<g:set var="runningParams" value="${filter ? filter + projParams : projParams}"/>
<g:if test="${scheduledExecution}">
    <g:set var="linkParams" value="${[jobIdFilter: scheduledExecution.id, includeJobRef: includeJobRef]+projParams}"/>
    <g:set var="runningParams" value="${[jobIdFilter: scheduledExecution.extid]+projParams}"/>
</g:if>
<div class="vue-tabs">
  <div class="nav-tabs-navigation">
    <div class="nav-tabs-wrapper">
      <ul class="nav nav-tabs activity_links">
          <li data-bind="css: { disabled: !nowRunningEnabled() }" class="active">
              <g:link controller="reports" action="index" class="running_link"
                      title="All activity for this job"
                      data-auto-refresh="5"
                      params="${runningParams + [runningFilter: 'running']}">
                  <i class="glyphicon glyphicon-play-circle"></i>
                  running
              </g:link>
          </li>
          <li>
              <g:link controller="reports" action="index" class="activity_link"
                      title="All activity for this job"
                      params="${linkParams}">
                  <i class="glyphicon glyphicon-time"></i>
                  recent
              </g:link>
          </li>

          <li>
              <g:link controller="reports" action="index" class="activity_link"
                      title="Failed executions"
                      params="${linkParams+[ statFilter: 'fail']}">
                  <i class="glyphicon glyphicon-minus-sign"></i>
                  failed
              </g:link>
          </li>

          <g:if test="${!execution || execution.user != session.user}">
              <li>
                  <g:link controller="reports" action="index" class="activity_link"
                          title="Executions by you"
                          params="${linkParams+[ userFilter: session.user]}">
                      <i class="glyphicon glyphicon-user"></i>
                      <g:message code="by.you" default="by you"/>
                  </g:link>
              </li>
          </g:if>

          <g:if test="${execution}">
              <li>
                  <g:link controller="reports" action="index" class="activity_link"
                          title="Executions by ${enc(attr:execution.user)}"
                          params="${linkParams+[ userFilter: execution.user]}">
                      <i class="glyphicon glyphicon-user"></i>
                      <g:message code="by" default="by"/>  <g:username user="${execution.user}"/>
                  </g:link>
              </li>
          </g:if>
      </ul>
    </div>
  </div>
  <g:if test="${knockoutBinding}">
  <div data-bind="visible: selected()" class="tab-content" style="display: none;">
    <div data-bind="visible: selected()" style="display: none; padding:0 5px 15px;">
      <a href="#" class="btn btn-default btn-xs" data-bind="attr: { href: href() } ">
          Filter Activity
      </a>
        <span data-bind="if: max() > 0" class="text-info" style="display:inline-block;padding-top:2px; margin-left: 1.5em;">
            Showing
            <span data-bind="text: results().length + ' of ' + total()"></span>
        </span>

        %{--bulk edit controls--}%
        <div class="pull-right">
            <span data-bind="visible: $root.bulkEditMode()" class="history_bulk_edit">
                <span class="btn btn-default btn-xs act_bulk_edit_selectall  " data-bind="click: bulkEditSelectAll">
                    <g:message code="select.all"/>
                </span>
                <span class="btn btn-default btn-xs act_bulk_edit_deselectall  " data-toggle="modal"
                      data-target="#cleanselections">
                    <g:message code="select.none"/>
                </span>

                <span class="btn btn-xs btn-danger btn-fill"
                      data-bind=" visible: $root.bulkEditMode(), attr: { disabled: (bulkEditIds().length<1  && bulkSelectedIds().length<1)}"
                      data-toggle="modal"
                      data-target="#bulkexecdelete">
                    <g:message code="delete.selected.executions"/>
                </span>
                <span class="btn btn-default btn-xs"
                      data-bind="click: $root.toggleBulkEdit, visible: $root.bulkEditMode()">
                    <g:message code="cancel.bulk.delete"/>
                </span>
            </span>

        <g:set var="projAdminAuth" value="${auth.resourceAllowedTest(
                context: 'application', type: 'project', name: params.project, action: AuthConstants.ACTION_ADMIN)}"/>
        <g:set var="deleteExecAuth" value="${auth.resourceAllowedTest(context: 'application', type: 'project', name:
                params.project, action: AuthConstants.ACTION_DELETE_EXECUTION) || projAdminAuth}"/>
        <g:if test="${deleteExecAuth}">
            <button class="btn btn-xs btn-warning"
                    data-bind="click: $root.toggleBulkEdit, visible: !$root.bulkEditMode()">
                <g:message code="bulk.delete"/>
            </button>
        </g:if>

            %{--confirm bulk delete modal--}%
            <div class="modal" id="bulkexecdelete" tabindex="-1" role="dialog"
                 aria-labelledby="bulkexecdeletetitle" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal"
                                    aria-hidden="true">&times;</button>
                            <h4 class="modal-title" id="bulkexecdeletetitle">Bulk Delete <g:message
                                    code="domain.Execution.title.plural" default="Executions"/></h4>
                        </div>

                        <div class="modal-body">

                            <p>Really delete <strong data-bind="text: $root.bulkSelectedIds().length"></strong>
                                <g:message code="domain.Execution.title.plural" default="Executions"/>?
                            </p>
                        </div>

                        <div class="modal-footer">

                                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                                    Cancel
                                </button>
                                <button class="btn btn-danger "
                                        data-bind="click: function(){$root.doBulkDelete('#bulkexecdelete','#bulkexecdeleteresult');}" >
                                    Delete Selected
                                </button>
                        </div>
                    </div>
                </div>
            </div>

        %{--confirm bulk delete modal--}%
            <div class="modal" id="cleanselections" tabindex="-1" role="dialog"
                 aria-labelledby="cleanselectionstitle" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal"
                                    aria-hidden="true">&times;</button>
                            <h4 class="modal-title" id="cleanselectionstitle">Clean bulk selection</h4>
                        </div>

                        <div class="modal-body">

                            <p>Clean all <strong data-bind="text: $root.bulkSelectedIds().length"></strong>
                                selections or clean only selections of actual page?
                            </p>
                        </div>

                        <div class="modal-footer">

                                <button type="submit" class="btn btn-default  " data-bind="click: $root.bulkEditDeselectAll">
                                    Only actual page
                                </button>
                                <button class="btn btn-danger "
                                        data-bind="click: $root.bulkEditDeselectAllPages" >
                                    All selections
                                </button>
                        </div>
                    </div>
                </div>
            </div>

            %{--bulk delete failure result modal--}%
            <div class="modal" id="bulkexecdeleteresult" tabindex="-1" role="dialog"
                 aria-labelledby="bulkexecdeleteresult-title" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal"
                                    aria-hidden="true">&times;</button>
                            <h4 class="modal-title" id="bulkexecdeleteresult-title">Bulk Delete <g:message
                                    code="domain.Execution.title.plural" default="Executions"/>: Results</h4>
                        </div>

                        <div class="modal-body" data-bind="visible: bulkEditProgress()">
                            <em>
                                <i class="glyphicon glyphicon-time text-info"></i>
                                Requesting bulk delete, please wait.
                            </em>
                        </div>
                        <div class="modal-body" data-bind="visible: !bulkEditProgress()">

                            <p
                                    data-bind="if: bulkEditResults() && bulkEditResults().requestCount && bulkEditResults().requestCount > 0"
                               class="text-info">
                                <strong data-bind="text: bulkEditResults().requestCount"></strong> Executions were
                            attempted.
                            </p>
                            <p data-bind="if: bulkEditResults() && bulkEditResults().successCount && bulkEditResults().successCount > 0"
                               class="text-success">
                                <strong data-bind="text: bulkEditResults().successCount"></strong> Executions were
                            successfully deleted.
                            </p>
                            <p data-bind="if: bulkEditResults() && bulkEditResults().failedCount && bulkEditResults().failedCount > 0"
                                    class="text-warning">
                                <strong data-bind="text: bulkEditResults().failedCount"></strong>  Executions could
                            not be deleted:
                            </p>
                            <div
                                    data-bind="if: bulkEditResults() && bulkEditResults().failures && bulkEditResults().failures.length>0">
                                <ul data-bind="foreach: bulkEditResults().failures">
                                    <li data-bind="text: message"></li>
                                </ul>
                            </div>

                            <div
                                    data-bind="if: bulkEditResults() && bulkEditResults().error">
                                  <p class="text-danger" data-bind="text: bulkEditResults().error"></p>
                            </div>

                        </div>

                        <div class="modal-footer">

                                <button type="submit" class="btn btn-default  " data-dismiss="modal">
                                    Close
                                </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div data-bind="visible: selected() && results().length < 1 " style="display: none;">
        <span class="text-primary" data-bind="if: !showReports()">No running executions found</span>
        <span class="text-primary" data-bind="if: showReports()">No matching activity found</span>
    </div>
    <table class=" table table-hover table-condensed events-table events-table-embed"
           style="width:100%; display: none"
           data-bind="visible: results().length > 0">
        <tbody class="no-border-on-first-tr" data-bind=" foreach: results ">
        <tr class="link activity_row autoclick"
            data-bind="css: { 'succeed': status()=='succeed', 'fail': status()=='fail', 'highlight': $root.highlightExecutionId()==executionId(), job: isJob(), adhoc: isAdhoc() } ">
            <td class="eventicon" data-bind="visible: $root.bulkEditMode()">
                <input type="checkbox" name="bulk_edit" data-bind="value: executionId(), checked: bulkEditSelected"
                       class="_defaultInput"/>
            </td>
            <td class="eventicon autoclickable" data-bind="attr: { 'title': executionState() } ">
                <i class="exec-status icon" data-bind="
                attr: { 'data-execstate': executionState, 'data-statusstring': customStatusString }
                "></i>
            </td>
            <td class="eventtitle autoclickable" data-bind="css: { job: isJob(), adhoc: isAdhoc() }">
                <a href="#" data-bind="text: '#'+executionId(), attr: { href: executionHref() }" class="_defaultAction"></a>
                <g:if test="${includeJobRef}">
                    <span data-bind="text: textJobRef('${scheduledExecution.extid}')"></span>
                </g:if>
                <g:if test="${showTitle}">
                    <span data-bind="if: !jobDeleted()">
                        <span data-bind="text: isJob()?jobName():executionString()"></span>
                    </span>
                    <span data-bind="if: jobDeleted()" class="text-primary">
                        (<g:message code="domain.ScheduledExecution.title"/>
                        <span data-bind="text: jobName()"></span>
                        has been deleted)
                    </span>
                </g:if>
                <span data-bind="if: isCustomStatus">
                    <span class="exec-status-text custom-status" data-bind="text: customStatusString"></span>
                </span>
            </td>
            <td class="eventargs autoclickable" >
                <div class="argstring-scrollable">
                <span data-bind="if: execution().jobArguments">
                    <span data-bind="foreachprop: execution().jobArguments">
                        <span data-bind="text: key"></span>:
                        <code data-bind="text: value" class="optvalue"></code>
                    </span>
                </span>
                <!-- ko if: !execution().jobArguments -->
                <span data-bind="text: execution().argString"></span>
                <!-- /ko -->
                </div>
            </td>
            <td class="right date autoclickable">
                <span data-bind="if: dateCompleted()">
                    <span class="timeabs" data-bind="text: endTimeFormat('${enc(attr:g.message(code:'jobslist.date.format.ko'))}')">

                    </span>
                    <span title="">
                        <span class="text-primary"><g:message code="in.of" default="in"/></span>
                        <span class="duration" data-bind="text: durationHumanize()"></span>
                    </span>
                </span>
                <span data-bind="if: !dateCompleted() && status() == 'scheduled'">
                    Scheduled; starting <span data-bind="text: timeToStart()"></span>
                </span>
                <span data-bind="if: !dateCompleted() && jobPercentageFixed() >= 0 && status() != 'scheduled'">
                    <div data-bind="if: isAdhoc() || jobAverageDuration()==0">
                    <g:render template="/common/progressBar" model="${[
                            indefinite: true, title: 'Running', innerContent: 'Running', width: 120,
                            progressClass: 'rd-progress-exec progress-striped active indefinite progress-embed',
                            progressBarClass: 'progress-bar-info',
                    ]}"/>
                    </div>
                    <div data-bind="if: isJob() && jobAverageDuration()>0">
                        <g:set var="progressBind" value="${', css: { \'progress-bar-info\': jobPercentageFixed() < 105 ,  \'progress-bar-warning\': jobPercentageFixed() > 104  }'}"/>
                        <g:render template="/common/progressBar"
                                  model="[completePercent: 0,
                                          progressClass: 'rd-progress-exec progress-embed',
                                          progressBarClass: '',
                                          containerId: 'progressContainer2',
                                          innerContent: '',
                                          showpercent: true,
                                          progressId: 'progressBar',
                                          bind: 'jobPercentageFixed()',
                                          progressBind: progressBind,
                                  ]"/>
                    </div>
                </span>
            </td>

            <td class="  user text-right autoclickable" style="white-space: nowrap;">
                <em><g:message code="by" default="by"/></em>
                <span data-bind="text: user"></span>
            </td>


        </tr>
        </tbody>
    </table>
    <div data-bind="if: pageCount() > 1">
      <ul class="pagination pagination-sm pagination-embed" data-bind="foreach: pageCount() > 1 ? pages() : []">
          <li data-bind="css: { active: $data.currentPage, disabled: $data.disabled } ">
              <a data-bind="attr: { href: ($data.skipped||$data.disabled||$data.currentPage)?'#':$data.url },
              click: function(){$data.skipped||$data.disabled||$data.currentPage?null:$root.visitPage($data);}">
                  <span data-bind="if: $data.nextPage">
                      <i class="glyphicon glyphicon-arrow-right"></i>
                  </span>
                  <span data-bind="if: $data.prevPage">
                      <i class="glyphicon glyphicon-arrow-left"></i>
                  </span>
                  <span data-bind="if: $data.skipped">
                      …
                  </span>
                  <span data-bind="if: $data.normal">
                      <span data-bind="text: $data.page">

                      </span>
                  </span>
              </a>
          </li>
      </ul>
    </div>


  </div>

  </g:if>
</div>

<g:jsonToken id="history_tokens" url="${request.forwardURI}"/>

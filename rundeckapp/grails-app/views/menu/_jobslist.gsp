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

<%@ page import="rundeck.ScheduledExecution; rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:set var="ukey" value="${g.rkey()}"/>
        <div class="jobslist ${small?'small':''}">
                <g:if test="${jobslist.size()>0}">
                    <% def j=0 %>
                    <g:each in="${runAuthRequired?jobslist.findAll{ jobauthorizations&&jobauthorizations[AuthConstants.ACTION_RUN]?.contains(it.id.toString())}:jobslist}" var="scheduledExecution">
                    <div class="list-group-item">
                        <g:set var="nextExecution"
                               value="${ (nextExecutions)? nextExecutions[scheduledExecution.id] : null}"/>
                        <g:set var="clusterUUID"
                               value="${ (clusterMap)? clusterMap[scheduledExecution.id] : null}"/>
                        <g:set var="currentTime" value="${new Date()}"/>
                        <g:set var="remoteClusterNodeUUID" value="${scheduledExecution.scheduled ? scheduledExecution.serverNodeUUID :null}" />
                        %{-- select job view --}%
                        <g:if test="${jobsjscallback}">
                            <div class=" expandComponentHolder expanded" id="jobrow_${scheduledExecution.id}">
                               <div class="jobname"
                                   data-job-id="${scheduledExecution.extid}"
                                   data-job-name="${scheduledExecution.jobName}"
                                   data-job-group="${scheduledExecution.groupPath}"
                                   style="overflow:hidden; text-overflow: ellipsis; white-space: nowrap; overflow-x: hidden">
                                       <g:set var="jstext" value="jobChosen('${enc(js: scheduledExecution.extid)}','${enc(js: scheduledExecution.jobName)}','${enc(js: scheduledExecution.groupPath)}',this)"/>
                                       <span class="textbtn textbtn-success" title="Choose this job" onclick="${enc(attr:jstext)}">
                                           <i class="glyphicon glyphicon-book"></i>
                                           <g:enc>${scheduledExecution.jobName}</g:enc>
                                       </span>

                                       <g:render template="/scheduledExecution/description"
                                                 model="[description: scheduledExecution?.description, textCss: 'text-secondary', firstLineOnly:true]"/>
                               </div>
                            </div>
                        </g:if>
                        <g:else>
                            %{--normal view--}%
                        <div class="sectionhead expandComponentHolder ${paginateParams?.idlist==scheduledExecution.id.toString()?'expanded':''}" id="jobrow_${scheduledExecution.id}">
                            <div class="jobname"
                                data-job-id="${scheduledExecution.extid}"
                                data-job-name="${scheduledExecution.jobName}"
                                data-job-group="${scheduledExecution.groupPath}"
                            >
                                <span class="jobbulkeditfield" style="display: none" data-bind="visible: enabled">
                                <input type="checkbox"
                                       name="ids"
                                       value="${enc(attr:scheduledExecution.extid)}"
                                       data-job-group="${scheduledExecution.groupPath}"
                                       class=" checkbox-inline"
                                />
                                </span>
                                    <span class="inlinebuttons jobbuttons">
                                        <g:if test="${scheduledExecution.hasExecutionEnabled() && jobauthorizations && jobauthorizations[AuthConstants.ACTION_RUN]?.contains(scheduledExecution.id.toString())}">
                                            <g:ifExecutionMode active="true" project="${scheduledExecution.project}">
                                            <g:link controller="scheduledExecution"
                                                    action="execute"
                                                    id="${scheduledExecution.extid}"
                                                    class=" btn btn-default btn-xs has_tooltip act_execute_job"
                                                    params="[project: scheduledExecution.project]"
                                                    data-toggle="tooltip"
                                                    data-placement="auto right"
                                                    title="Choose options and Run Job"
                                                    data-job-id="${scheduledExecution.extid}"
                                            >
                                                <b class="glyphicon glyphicon-play"></b>
                                            </g:link>
                                            </g:ifExecutionMode>
                                            <g:ifExecutionMode passive="true" project="${scheduledExecution.project}">
                                                <span title="${g.message(code: 'disabled.job.run')}"
                                                      class="has_tooltip"
                                                      data-toggle="tooltip"
                                                      data-placement="auto bottom"
                                                >%{--Extra span because .disabled will cancel tooltip from showing --}%
                                                    <span class="btn btn-default btn-xs disabled ">
                                                        <b class="glyphicon glyphicon-play"></b>
                                                    </span>
                                                </span>
                                            </g:ifExecutionMode>
                                        </g:if>
                                    </span>

                                <g:set var="exportstatus" value="${scmExportEnabled ? scmStatus?.get(scheduledExecution.extid):null}"/>
                                <g:set var="importStatus" value="${scmImportEnabled ? scmImportJobStatus?.get(scheduledExecution.extid): null}"/>
                                <g:if test="${exportstatus || importStatus}">

                                    <g:render template="/scm/statusBadge"
                                              model="[exportStatus: exportstatus?.synchState?.toString(),
                                                      importStatus: importStatus?.synchState?.toString(),
                                                      text  : '',
                                                      notext: true,
                                                      exportCommit  : exportstatus?.commit,
                                                      importCommit  : importStatus?.commit,
                                              ]"/>
                                </g:if>

                                <!-- ko if: displayBadge('${scheduledExecution.extid}') -->
                                <span data-bind="attr: {'title': jobText('${scheduledExecution.extid}') }" class="has_tooltip">
                                    <span data-bind="css: jobClass('${scheduledExecution.extid}')">
                                        <i data-bind="css: jobIcon('${scheduledExecution.extid}')" class="glyphicon "></i>
                                    </span>
                                </span>
                                <!-- /ko -->

                                <div class="btn-group pull-right">
                                    <button type="button"
                                            class="btn btn-default btn-sm dropdown-toggle act_job_action_dropdown"
                                            title="${g.message(code: 'click.for.job.actions')}"
                                            data-job-id="${enc(attr:scheduledExecution.extid)}"
                                            data-toggle="dropdown"
                                            aria-expanded="false">
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu" role="menu">
                                        <li role="presentation" class="dropdown-header"><g:message code="loading.text" /></li>
                                    </ul>
                                </div>
                                <g:link action="show"
                                        controller="scheduledExecution"
                                        id="${scheduledExecution.extid}"
                                        class="hover_show_job_info"
                                        params="[project: scheduledExecution.project]"
                                        data-job-id="${scheduledExecution.extid}">
                                    <g:if test="${showIcon}">
                                        <i class="glyphicon glyphicon-book"></i>
                                    </g:if>
                                    <g:enc>${scheduledExecution.jobName}</g:enc>
                                </g:link>
                                <div>
                                  <g:render template="/scheduledExecution/description"
                                            model="[description: scheduledExecution?.description,
                                                    textCss:'text-secondary',
                                                    mode:'collapsed',
                                                    rkey:g.rkey(),
                                                    jobLinkId:scheduledExecution?.extid,
                                                    cutoffMarker: ScheduledExecution.RUNBOOK_MARKER
                                            ]"/>
                                </div>
                            </div>
                            <g:if test="${scheduledExecution.scheduled}">
                            <div class="scheduletime" style="margin-top:5px; margin-left:5px;">
                                <g:if test="${scheduledExecution.scheduled && nextExecution}">
                                    <g:if test="${serverClusterNodeUUID && !remoteClusterNodeUUID}">
                                        <span class="text-warning has_tooltip" title="${message(code:"scheduledExecution.scheduled.cluster.orphan.title")}"
                                              data-placement="right"
                                        >
                                            <g:icon name="alert"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <g:icon name="time"/>
                                    </g:else>
                                    <span title="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0", args:[remoteClusterNodeUUID]) : ''} at ${g.relativeDate(atDate: nextExecution)}">
                                        <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                                    </span>

                                    <g:if test="${remoteClusterNodeUUID}">
                                        on
                                        <span data-server-uuid="${remoteClusterNodeUUID}" data-server-name=" " class="rundeck-server-uuid text-primary">
                                        </span>
                                    </g:if>
                                </g:if>
                                <g:elseif test="${scheduledExecution.scheduled && !g.executionMode(is:'active', project:scheduledExecution.project)}">
                                    <span class="scheduletime disabled has_tooltip" data-toggle="tooltip"
                                          data-placement="auto right"
                                          title="${g.message(code: 'disabled.schedule.run')}">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="disabled" /></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${!scheduledExecution.hasScheduleEnabled() && scheduledExecution.hasExecutionEnabled()}">
                                    <span class="scheduletime disabled has_tooltip"
                                          title="${g.message(code: 'scheduleExecution.schedule.disabled')}"
                                          data-toggle="tooltip"
                                          data-placement="auto right">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${scheduledExecution.hasScheduleEnabled() && !g.scheduleMode(is:'active', project:scheduledExecution.project)}">
                                    <span class="scheduletime disabled has_tooltip"
                                          title="${g.message(code: 'project.schedule.disabled')}"
                                          data-toggle="tooltip"
                                          data-placement="auto left">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
                                    <span class="scheduletime willnotrun has_tooltip"
                                          title="${g.message(code: 'job.schedule.will.never.fire')}"
                                          data-toggle="tooltip"
                                          data-placement="auto left">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                            </div>
                            </g:if>
                        </div>
                        </g:else>
                        <% j++ %>
                        </div>
                    </g:each>
            </g:if>
                <g:else>
                <span class="note empty">None</span>
            </g:else>
                <g:if test="${total && max && total.toInteger() > max.toInteger() && max.toInteger() > 0 && !hideSummary}">
                    <span class="info note">Showing <g:enc>${jobslist.size()} of ${total}</g:enc></span>
                </g:if>
        </div>

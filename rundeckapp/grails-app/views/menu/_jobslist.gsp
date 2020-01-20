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

<%@ page import="rundeck.ScheduledExecution; rundeck.Execution; org.rundeck.core.auth.AuthConstants" %>

<g:set var="ukey" value="${g.rkey()}"/>
        <div class="jobslist ${small?'small':''}">
                <g:if test="${jobslist.size()>0}">
                    <% def j=0 %>
                    <g:each in="${runAuthRequired?jobslist.findAll{ jobauthorizations&&jobauthorizations[AuthConstants.ACTION_RUN]?.contains(it.id.toString())}:jobslist}" var="scheduledExecution">
                    <div class="">
                        <g:set var="nextExecution"
                               value="${ (nextExecutions)? nextExecutions[scheduledExecution.id] : null}"/>
                        <g:set var="calendar"
                               value="${ (calendars)? calendars[scheduledExecution.id] : null}"/>
                       <g:set var="scheduleNames" value="${scheduledExecution.getScheduleDefinitionNamesConcatenated()}"></g:set>
                        <g:set var="clusterUUID"
                               value="${ (clusterMap)? clusterMap[scheduledExecution.id] : null}"/>
                        <g:set var="currentTime" value="${new Date()}"/>
                        <g:set var="remoteClusterNodeUUID" value="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions) ? scheduledExecution.serverNodeUUID :null}" />
                        %{-- select job view --}%
                        <g:if test="${jobsjscallback}">
                            <div class=" expandComponentHolder expanded" id="jobrow_${scheduledExecution.id}">
                               <div class="jobname job_list_row"
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
                            <div class="jobname job_list_row hover-reveal-hidden"
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
                                                    class=" btn btn-success btn-simple btn-hover btn-xs act_execute_job"
                                                    params="[project: scheduledExecution.project]"
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
                                        <g:else>
                                            <span class=" text-muted disabled   act_execute_job" style=" padding: 4px 5px;"
                                                title="Cannot run job" disabled>
                                                <b class="glyphicon glyphicon-minus"></b>
                                            </span>
                                        </g:else>
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

                                <div class="btn-group pull-right visibility-hidden">
                                    <button type="button"
                                            class="btn btn-secondary btn-xs dropdown-toggle act_job_action_dropdown"
                                            data-job-id="${enc(attr:scheduledExecution.extid)}"
                                            data-toggle="dropdown"
                                            aria-expanded="false">
                                            <g:message code="actions" />
                                        <span class="caret"></span>
                                    </button>
                                    <ul class="dropdown-menu" role="menu">
                                        <li role="presentation" class="dropdown-header"><g:message code="loading.text" /></li>
                                    </ul>
                                </div>
                                <g:link action="show"
                                        controller="scheduledExecution"
                                        id="${scheduledExecution.extid}"
                                        class="hover_show_job_info text-primary"
                                        params="[project: scheduledExecution.project]"
                                        data-job-id="${scheduledExecution.extid}">
                                    <g:if test="${showIcon}">
                                        <i class="glyphicon glyphicon-book"></i>
                                    </g:if>
                                    <g:enc>${scheduledExecution.jobName}</g:enc>
                                </g:link>

                                  <g:render template="/scheduledExecution/description"
                                            model="[description: scheduledExecution?.description,
                                                    textCss:'text-secondary',
                                                    mode:'collapsed',
                                                    firstLineOnly:true,
                                                    rkey:g.rkey(),
                                                    jobLinkId:scheduledExecution?.extid,
                                                    cutoffMarker: ScheduledExecution.RUNBOOK_MARKER
                                            ]"/>


                            <g:if test="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions)}">
                            <span class="scheduletime" >
                                <g:if test="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions) && nextExecution}">
                                    <g:if test="${serverClusterNodeUUID && !remoteClusterNodeUUID}">
                                        <span class="text-warning has_tooltip" title="${message(code:"scheduledExecution.scheduled.cluster.orphan.title")}"
                                              data-placement="right"
                                        >
                                            <g:icon name="alert"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <g:if test="${scheduleNames}">
                                            <span class="text-success has_tooltip" title="${message(code:"scheduledExecution.scheduled.schedule.title")} <br>${scheduleNames}"
                                                  data-placement="right">
                                                <g:icon name="time"/>
                                            </span>
                                        </g:if>
                                        <g:else>
                                            <span class="text-success has_tooltip" data-placement="right">
                                                <g:icon name="time"/>
                                            </span>
                                        </g:else>

                                        <g:if test="${calendar}">
                                            <span class="text-success has_tooltip" title="${message(code:"scheduledExecution.scheduled.calendar.title")} <br>${calendar}"
                                                  data-placement="right">
                                                <i class="glyphicon glyphicon-calendar"></i>
                                            </span>
                                        </g:if>

                                    </g:else>
                                    <span title="${remoteClusterNodeUUID ? g.message(code: "scheduled.to.run.on.server.0", args:[remoteClusterNodeUUID]) : ''} at ${g.relativeDate(atDate: nextExecution)}"
                                            class="text-secondary">
                                        <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil text-success"/>
                                    </span>

                                </g:if>
                                <g:elseif test="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions) && !g.executionMode(is:'active', project:scheduledExecution.project)|| !scheduledExecution.hasExecutionEnabled()}">
                                    <span class="scheduletime disabled has_tooltip text-secondary" data-toggle="tooltip"
                                          data-placement="auto right"
                                          title="${g.message(code: 'disabled.schedule.run')}">
                                        <i class="glyphicon glyphicon-pause"></i>
                                        <span class="detail"><g:message code="disabled" /></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${!scheduledExecution.hasScheduleEnabled() && scheduledExecution.hasExecutionEnabled()}">
                                    <span class="scheduletime disabled has_tooltip text-secondary"
                                          title="${g.message(code: 'scheduleExecution.schedule.disabled')}"
                                          data-toggle="tooltip"
                                          data-placement="auto right">
                                        <i class="glyphicon glyphicon-pause"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${scheduledExecution.hasScheduleEnabled() && !g.scheduleMode(is:'active', project:scheduledExecution.project) }">
                                    <span class="scheduletime disabled has_tooltip text-secondary"
                                          title="${g.message(code: 'project.schedule.disabled')}"
                                          data-toggle="tooltip"
                                          data-placement="auto left">
                                        <i class="glyphicon glyphicon-pause"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${(scheduledExecution.scheduled || scheduledExecution.scheduleDefinitions) && !nextExecution}">
                                    <span class="scheduletime willnotrun has_tooltip text-warning"
                                          title="${g.message(code: 'job.schedule.will.never.fire')}"
                                          data-toggle="tooltip"
                                          data-placement="auto left">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="never"/></span>
                                    </span>
                                </g:elseif>
                            </span>
                            </g:if>
                            </div>
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

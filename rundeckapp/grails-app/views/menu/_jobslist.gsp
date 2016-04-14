<%@ page import="rundeck.Execution; com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:set var="ukey" value="${g.rkey()}"/>
        <div class="jobslist ${small?'small':''}">
                <g:if test="${jobslist.size()>0}">
                <table cellpadding="0" cellspacing="0" class="jobsList list" >
                    <g:if test="${null==headers || headers}">
                <tr>
                    <g:if test="${sortEnabled}">

                        <g:sortableColumn property="jobName" defaultOrder="desc" title="Name" colspan="2"  params="${paginateParams}"/>
                        <g:sortableColumn property="nextExecution" defaultOrder="desc" title="Execution" colspan="1" params="${paginateParams}"/>
                    </g:if>
                    <g:else>

                        <th colspan="2">Name</th>
                        <th colspan="1">Execution</th>
                    </g:else>
                </tr>
                        </g:if>
                    <% def j=0 %>
                    <g:each in="${runAuthRequired?jobslist.findAll{ jobauthorizations&&jobauthorizations[AuthConstants.ACTION_RUN]?.contains(it.id.toString())}:jobslist}" var="scheduledExecution">
                        <g:set var="nextExecution"
                               value="${ (nextExecutions)? nextExecutions[scheduledExecution.id] : null}"/>
                        <g:set var="clusterUUID"
                               value="${ (clusterMap)? clusterMap[scheduledExecution.id] : null}"/>
                        <g:set var="currentTime" value="${new Date()}"/>
                        <g:set var="remoteClusterNodeUUID" value="${scheduledExecution.scheduled ? scheduledExecution.serverNodeUUID :null}" />
                        %{-- select job view --}%
                        <g:if test="${jobsjscallback}">
                            <tr class=" expandComponentHolder expanded" id="jobrow_${scheduledExecution.id}">
                               <td class="jobname" style="overflow:hidden; text-overflow: ellipsis; white-space: nowrap; overflow-x: hidden">
                                       <g:set var="jstext" value="jobChosen('${enc(js: scheduledExecution.jobName)}','${enc(js: scheduledExecution.groupPath)}')"/>
                                       <span class="textbtn textbtn-success" title="Choose this job" onclick="${enc(attr:jstext)}">
                                           <i class="glyphicon glyphicon-book"></i>
                                           <g:enc>${scheduledExecution.jobName}</g:enc>
                                       </span>

                                       <g:render template="/scheduledExecution/description"
                                                 model="[description: scheduledExecution?.description, textCss: 'text-muted', firstLineOnly:true]"/>
                               </td>
                            </tr>
                        </g:if>
                        <g:else>
                            %{--normal view--}%
                        <tr class="sectionhead expandComponentHolder ${paginateParams?.idlist==scheduledExecution.id.toString()?'expanded':''}" id="jobrow_${scheduledExecution.id}">
                            <td class="jobname">
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
                                            <g:ifExecutionMode active="true">
                                            <g:link controller="scheduledExecution"
                                                    action="execute"
                                                    id="${scheduledExecution.extid}"
                                                    class=" btn btn-default btn-xs has_tooltip act_execute_job"
                                                    params="[project: scheduledExecution.project]"
                                                    data-toggle="tooltip"
                                                    title="Choose options and Run Job…"
                                                    data-job-id="${scheduledExecution.extid}"
                                            >
                                                <b class="glyphicon glyphicon-play"></b>
                                            </g:link>
                                            </g:ifExecutionMode>
                                            <g:ifExecutionMode passive="true">
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
                                    <g:link action="show"
                                            controller="scheduledExecution"
                                            id="${scheduledExecution.extid}"
                                            class="hover_show_job_info"
                                            params="[project: scheduledExecution.project]">
                                        <g:if test="${showIcon}">
                                            <i class="glyphicon glyphicon-book"></i>
                                        </g:if>
                                        <g:enc>${scheduledExecution.jobName}</g:enc></g:link>
                                <div class="btn-group">
                                    <button type="button"
                                            class="btn btn-default btn-sm btn-link dropdown-toggle act_job_action_dropdown"
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

                                <g:render template="/scheduledExecution/description"
                                          model="[description: scheduledExecution?.description,textCss:'text-muted',mode:'collapsed',rkey:g.rkey()]"/>

                            </td>
                            <g:if test="${scheduledExecution.scheduled}">
                            <td class="scheduletime">
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
                                        <span data-server-uuid="${remoteClusterNodeUUID}" data-server-name=" " class="rundeck-server-uuid text-muted">
                                        </span>
                                    </g:if>
                                </g:if>
                                <g:elseif test="${scheduledExecution.scheduled && !g.executionMode(is:'active')}">
                                    <span class="scheduletime disabled has_tooltip" data-toggle="tooltip"
                                          data-placement="auto left"
                                          title="${g.message(code: 'disabled.schedule.run')}">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <span class="detail"><g:message code="disabled" /></span>
                                    </span>
                                </g:elseif>
                                <g:elseif test="${!scheduledExecution.hasScheduleEnabled() && scheduledExecution.hasExecutionEnabled()}">
                                    <span class="scheduletime disabled has_tooltip"
                                          title="${g.message(code: 'scheduleExecution.schedule.disabled')}"
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
                            </td>
                            </g:if>
                        </tr>
                        </g:else>
                        <% j++ %>
                    </g:each>
                </table>
            </g:if>
                <g:else>
                <span class="note empty">None</span>
            </g:else>
                <g:if test="${total && max && total.toInteger() > max.toInteger() && max.toInteger() > 0 && !hideSummary}">
                    <span class="info note">Showing <g:enc>${jobslist.size()} of ${total}</g:enc></span>
                </g:if>
        </div>

<g:set var="timeNow" value="${new Date().getTime()}"/>
<%
    def runcount = 0;
%>
<g:if test="${executions?.size()>0}">
    <table cellpadding="0" cellspacing="0" class="jobsList ${small ? 'small' : ''}" width="100%">
        <% def j = 0 %>
        <g:each in="${executions}" var="execution">
            <g:set var="scheduledExecution" value="${jobs[execution.scheduledExecution?.id.toString()]}"/>
            <g:set var="execstatus" value="${execution.dateCompleted?(execution.status=='true'?'succeeded':execution.cancelled?'killed':'failed'):'alive'}"/>

            <g:set var="execLink" value="${createLink(controller:'execution',action:'show', id:execution.id)}"/>

            <tr class=" ${j % 2 == 1 ? 'alternateRow' : ''}  ${!execution.dateCompleted ? 'nowrunning' : ''} execution ${execstatus} hilite link"
                id="${upref}exec-${execution.id}-row" onclick="document.location='${execLink}';">

                <g:if test="${scheduledExecution}">
                    <td class="jobname">
                        %{--<g:link title="${'View '+g.message(code:'domain.ScheduledExecution.title')}" controller="scheduledExecution" action="show" id="${scheduledExecution.id}">${scheduledExecution.jobName.encodeAsHTML()}</g:link>--}%
                        <g:link title="View execution output" controller="execution" style="display:block" action="show" id="${execution.id}">${scheduledExecution.jobName.encodeAsHTML()}</g:link>
                    </td>
                    <td class="jobdesc">${scheduledExecution.description?.encodeAsHTML()}</td>
                </g:if>
                <g:else>
                    <td class="jobname transient ">
                        <g:message code="events.history.jobname.adhoc"/>
                    </td>
                    <td class="jobdesc">

                    </td>
                </g:else>

                <g:if test="${!small}">
                    <td class="dateStarted  " title="started: ${execution.dateStarted}">
                        <span class="timelabel">started:</span> <g:relativeDate atDate="${execution.dateStarted}"/>
                        (<g:relativeDate elapsed="${execution.dateStarted}" />)
                    </td>
                </g:if>

                <td class="runstatus" colspan="${!execution.dateCompleted && !small ? '2' : '1'}">

                    <g:if test="${execution.dateCompleted}">
                        <span class="timelabel" title="completed: ${execution.dateCompleted}">
                            <g:if test="${execution.status=='true'}">
                                completed:
                            </g:if>
                            <g:elseif test="${execution.cancelled}">
                                killed:
                            </g:elseif>
                            <g:else>
                                failed:
                            </g:else>
                        </span>
                        <span class="completedTime" title="completed: ${execution.dateCompleted}">
                            <g:relativeDate atDate="${execution.dateCompleted}"/>
                        </span>
                        <span class=" duration">
                            <g:if test="${!small}">
                                <span class="timelabel">duration:</span>
                            </g:if>
                            (${execution.durationAsString()})
                        </span>
                    </g:if>
                    <g:else>
                    %{--<span class="dateCompleted" colspan="2"  id="exec-${execution.id}-dateCompleted">--}%
                        <g:if test="${scheduledExecution && scheduledExecution.execCount>0 && scheduledExecution.totalTime > 0 && execution.dateStarted}">
                            <g:set var="avgTime" value="${(Long)(scheduledExecution.totalTime/scheduledExecution.execCount)}"/>
                            <g:set var="completePercent" value="${(int)Math.floor((double)(100 * (timeNow - execution.dateStarted.getTime())/(avgTime)))}"/>
                            <g:set var="estEndTime" value="${(long)(execution.dateStarted.getTime() + (long)avgTime)}"/>
                            <g:set var="completeEstimate" value="${new Date(estEndTime)}"/>
                            <g:set var="completeEstimateTime" value="${g.relativeDate(atDate:completeEstimate)}"/>
                            <g:if test="${estEndTime>timeNow}">
                                <g:set var="completeRemaining" value="${g.timeDuration(start:new Date(timeNow),end:completeEstimate)}"/>
                            </g:if>
                            <g:else>
                                <g:set var="completeRemaining" value="${'+'+g.timeDuration(start:completeEstimate,end:new Date(timeNow))}"/>
                            </g:else>
                            <g:render template="/common/progressBar" model="${[completePercent:(int)completePercent,title:completePercent < 100 ? 'Estimated completion time: ' + completeEstimateTime : '',
                                showpercent:true,showOverrun:true,remaining:' ('+completeRemaining+')']}"/>
                        </g:if>
                        <g:else>
                            <g:render template="/common/progressBar" model="${[indefinite:true,title:'running',innerContent:'running']}"/>
                        </g:else>
                    </g:else>
                </td>

                <td class="user">
                    ${execution.user}
                </td>

                <td style="padding:0px" class="sepL outputlink hilite action ${!execution.dateCompleted ? 'nowrunning' : ''}">
                    <g:link title="View execution output" controller="execution" style="display:block;padding:5px" action="show" id="${execution.id}">output &raquo;</g:link>
                </td>

            </tr>
            <% j++ %>

        </g:each>
    </table>
</g:if>
<g:else>
    <span class="note empty">${emptyText ? emptyText : 'None'}</span>
</g:else>
<script language="text/javascript">
    if (typeof(updateNowRunning) == 'function') {
        updateNowRunning(<%=executions?.size()%>);
    }
</script>
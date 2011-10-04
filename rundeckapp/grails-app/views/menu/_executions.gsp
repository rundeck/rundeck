<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %><g:set var="timeNow" value="${new Date().getTime()}"/>
<%
    def runcount=0;
%>
<g:if test="${executions?.size()>0}">
                <table cellpadding="0" cellspacing="0"  class="jobsList ${small?'small':''}" width="100%">
                    <% def j=0 %>
                    <g:each in="${executions}" var="execution">
                            <g:set var="scheduledExecution" value="${jobs[execution.scheduledExecution?.id.toString()]}"/>
                            <g:set var="execstatus" value="${execution.dateCompleted?(execution.status=='true'?'succeeded':execution.cancelled?'killed':'failed'):'alive'}"/>

                            <tr class=" ${j%2==1?'alternateRow':''}  ${!execution.dateCompleted?'nowrunning':''} execution ${execstatus}"
                                id="${upref}exec-${execution.id}-row"
                            >
                                <td id="${upref}exec-${execution.id}-spinner" class="espinner" >
                                    <g:if test="${execution.dateCompleted}">

                                     </g:if>
                                    <g:else>
                                        <%
                                            runcount++;
                                        %>
                                        
                                    </g:else>
                                </td>

                                <td>
                                    <g:if test="${execution.dateCompleted}">

                                        <span title="finished: <g:relativeDate atDate='${execution.dateCompleted}'/>">
                                        <g:relativeDate elapsed="${execution.dateCompleted}" agoClass="timeuntil"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <g:link class="timenow"
                                            controller="execution"
                                            action="show"
                                            title="View execution output"
                                            id="${execution.id}">
                                                now
                                        </g:link>
                                    </g:else>
                                </td>


                                    <g:if test="${scheduledExecution}">
                                        <td class="jobname">
                                            <g:link
                                            title="${'View '+g.message(code:'domain.ScheduledExecution.title')}" controller="scheduledExecution" action="show" id="${scheduledExecution.extid}">${scheduledExecution.jobName.encodeAsHTML()}</g:link>
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
                                <td class="dateStarted  sepL" title="started: ${execution.dateStarted}">
                                    <span class="timelabel">started:</span> <g:relativeDate atDate="${execution.dateStarted}" />
                                </td>
                                </g:if>

                                <td class="runstatus" colspan="${!execution.dateCompleted && !small?'2':'1'}">

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
                                    <span class="completedTime" title="completed: ${execution.dateCompleted}" >
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
                                        <g:set var="completeEstimate" value="${new Date(execution.dateStarted.getTime() + (long)avgTime)}"/>
                                        <g:set var="completeEstimateTime" value="${g.relativeDate(atDate:completeEstimate)}"/>
                                        <g:set var="completeRemaining" value="${g.timeDuration(start:new Date(timeNow),end:new Date(((long)execution.dateStarted.getTime()) + (long)avgTime))}"/>
                                        <div id="exec-${execution.id}-progressContainer" class="progressContainer small" >
                                            <div
                                                id="exec-${execution.id}-progressBar"
                                                class="progressBar"
                                                title="${completePercent<100?'Estimated completion time: '+completeEstimateTime:''}"
                                                style="width:${completePercent>100?'100':completePercent}"
                                            >${completePercent>100?'100':completePercent}%${completePercent<100?'&nbsp;('+completeRemaining+')':''}</div>
                                        </div>
                                        </g:if>
                                        <g:else>
                                            <span class="timenow">
                                                running
                                            </span>
                                        </g:else>
                                    <!--</td>-->
                                    <span id="cancelcol-${execution.id}" colspan="2">
                                        <auth:jobAllowed job="${scheduledExecution?scheduledExecution:[jobName:'adhoc',groupPath:'adhoc']}" name="${AuthConstants.ACTION_KILL}">
                                        <span  class="action button small textbtn" id="cancellink-${execution.id}" onclick="['cancelconf-${execution.id}','cancellink-${execution.id}'].each(Element.toggle)">Kill <g:if test="${!small}"><g:message code="domain.ScheduledExecution.title"/> Now</g:if> <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="" width="12px" height="12px"/></span>
                                        <span id="cancelconf-${execution.id}" style="display:none" class="confirmMessage">
                                            Really kill this job?

                                            <span  class="action button small textbtn" onclick="['cancelconf-${execution.id}','cancellink-${execution.id}'].each(Element.toggle);">No</span>
                                            <span  class="action button small textbtn" onclick="canceljob('${execution.id}','cancelcol-${execution.id}');">Yes</span>
                                        </span>
                                        </auth:jobAllowed>
                                    </span>
                                </g:else>
                                </td>
                                <g:if test="${execution.dateCompleted}">
                                    <g:if test="${!small}">
                                    <td>
                                        <span style="padding:5px" class="sepL outputlink hilite action  ${execution.dateCompleted && execution?.status!='true'?'fail':''} ${!execution.dateCompleted?'nowrunning':''}"  >
                                            <g:link title="Download execution output" controller="execution" action="downloadOutput" id="${execution.id}"><img src="${resource(dir:'images',file:'icon-small-file.png')}" alt="Download" title="Download output" width="13px" height="16px"/></g:link>
                                        </span>
                                    </td>
                                    </g:if>
                                </g:if>
                                <td class="sepL user">
                                    ${execution.user}
                                </td>

                                <td style="padding:0px" class="sepL outputlink hilite action ${!execution.dateCompleted?'nowrunning':''}"  >
                                    <g:link title="View execution output" controller="execution" style="display:block;padding:5px" action="show" id="${execution.id}">output &raquo;</g:link>
                                </td>

                            </tr>
                            <% j++ %>
                        
                    </g:each>
                </table>
            </g:if>
            <g:else>
                    <span class="note empty">${emptyText?emptyText:'None'}</span>
            </g:else>
<script language="text/javascript">
    if(typeof(updateNowRunning)=='function'){
        updateNowRunning(<%=runcount%>);
    }
</script>
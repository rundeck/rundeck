<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %><g:if test="${!options}">
    <g:set var="options" value="[job:true,user:true,stat:true,out:true,duration:true,cmdinf:true]"/>
</g:if>
<g:if test="${executions?.size()>0}">
                <table cellpadding="0" cellspacing="0" style="min-width: 500px" class="jobsList">
                    <thead>
                        <g:if test="${options?.stat}">
                            <th colspan="2">Result</th>
                        </g:if>

                        <g:if test="${options?.job}">
                            <th><g:message code="domain.ScheduledExecution.title"/> Name</th>
                        </g:if>
                        <g:if test="${options?.desc}">
                            <th>Desc</th>
                        </g:if>
                        <g:if test="${options?.user}">
                            <th>User</th>
                        </g:if>

                        <g:if test="${options?.start}">
                        <th>Started</th>
                        </g:if>
                        <g:if test="${options?.end}">
                        <th colspan="2">Finished</th>
                        </g:if>
                        <g:if test="${options?.duration}">
                            <th>Duration</th>
                        </g:if>

                        <g:if test="${options?.objinf}">
                        <th>Resoure</th>
                        </g:if>

                        <g:if test="${options?.cmdinf}">
                        <th>Command</th>
                        </g:if>

                        <g:if test="${options?.out}">
                        <th colspan="2"">Output</th>
                        </g:if>

                    </thead>
                    <% def j=0 %>
                    <g:each in="${executions}" var="execution">
                            <g:set var="scheduledExecution" value="${jobs[execution.scheduledExecution?.id.toString()]}"/>

                            <tr class=" ${j%2==1?'alternateRow':''}  ${execution.dateCompleted && execution?.status!='true'?'fail':''}  ${!execution.dateCompleted?'nowrunning':''}">
                                <g:if test="${options?.stat}">
                                <td >
                                    <g:if test="${execution.dateCompleted}">
                                    <img
                                        src="${resource(dir:'images',file:'icon-tiny-'+(execution?.status=='true'?'ok':'warn')+'.png')}"
                                        title="${execution?.cancelled?'Killed':execution?.status=='true'?'Succeeded':'Failed'}: <g:relativeDate atDate='${execution.dateCompleted}'/>"
                                        alt="" width="12px" height="12px"/>
                                     </g:if>
                                    <g:else>
                                        <g:link class="timenow"
                                            controller="execution"
                                            action="show"
                                            title="View execution output"
                                            id="${execution.id}">
                                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt=""/>
                                        </g:link>
                                    </g:else>
                                </td>

                                <td>
                                    <g:if test="${execution.dateCompleted}">

                                        <span title="${execution?.cancelled?'Killed':execution?.status=='true'?'Succeeded':'Failed'}: <g:relativeDate atDate='${execution.dateCompleted}'/>">
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
                                </g:if>

                                    <g:if test="${scheduledExecution}">
                                        <g:if test="${options?.job}">
                                        <td class="jobname ${execution?.status!='true'?'fail':''}">
                                            <g:link
                                            title="${'View '+g.message(code:'domain.ScheduledExecution.title')}" controller="scheduledExecution" action="show" id="${scheduledExecution.extid}">${scheduledExecution.jobName.encodeAsHTML()}</g:link >
                                        </td>
                                        </g:if>
                                        <g:if test="${options?.desc}">
                                        <td class="jobdesc">${scheduledExecution.description?.encodeAsHTML()}</td>
                                        </g:if>
                                    </g:if>
                                    <g:else>
                                        <g:if test="${options?.job}">
                                        <td class="jobname transient ${execution?.status!='true'?'fail':''}">
                                            transient <g:message code="domain.ScheduledExecution.title"/>
                                        </td>
                                        </g:if>
                                        <g:if test="${options?.desc}">
                                        <td class="jobdesc">
                                            ${options?.desc}
                                        </td>
                                        </g:if>
                                    </g:else>
                                <g:if test="${options?.user}">
                                <td class="sepL user">
                                    ${execution.user}
                                </td>
                                </g:if>
                                <g:if test="${options?.start}">
                                <td class="sepL dateStarted" title="started: ${execution.dateStarted}">
                                    <g:relativeDate atDate="${execution.dateStarted}" />
                                </td>
                                </g:if>

                                <g:if test="${execution.dateCompleted}">
                                    <g:if test="${options?.end}">
                                    <td class="sepL dateCompletedlabel" title="completed: ${execution.dateCompleted}">
                                         <span class="timelabel">
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
                                        </td>
                                    <td  title="completed: ${execution.dateCompleted}" >
                                        <g:relativeDate atDate="${execution.dateCompleted}"/>

                                    </td>
                                    </g:if>
                                    <g:if test="${options?.duration}">
                                    <td class="sepL duration">
                                        ${execution.durationAsString()}
                                    </td>
                                    </g:if>
                                </g:if>
                                <g:else>
                                    <g:if test="${options?.end}">
                                    <td class="sepL dateCompleted" colspan="2"  id="exec-${execution.id}-dateCompleted">
                                    <span class="timenow">
                                        now running
                                    </span>
                                    </td>
                                    </g:if>
                                    <g:if test="${options?.duration}">
                                    <td id="cancelcol-${execution.id}">
                                        <auth:jobAllowed job="${scheduledExecution}" name="${AuthConstants.ACTION_KILL}">
                                        <span  class="action button small textbtn" id="cancellink-${execution.id}" onclick="['cancelconf-${execution.id}','cancellink-${execution.id}'].each(Element.toggle)">Kill <g:message code="domain.ScheduledExecution.title"/> Now <img src="${resource(dir:'images',file:'icon-tiny-removex.png')}" alt="" width="12px" height="12px"/></span>
                                        <span id="cancelconf-${execution.id}" style="display:none" class="confirmMessage">
                                            Really kill this job?

                                            <span  class="action button small textbtn" onclick="['cancelconf-${execution.id}','cancellink-${execution.id}'].each(Element.toggle);">No</span>
                                            <span  class="action button small textbtn" onclick="canceljob('${execution.id}','cancelcol-${execution.id}');">Yes</span>
                                        </span>
                                        </auth:jobAllowed>


                                    </td>
                                    </g:if>
                                </g:else>

                                
                                <g:if test="${options?.out}">
                                    <td style="padding:0px" class="sepL outputlink hilite action  ${execution.dateCompleted && execution?.status!='true'?'fail':''} ${!execution.dateCompleted?'nowrunning':''}"  >
                                        <g:link title="Download execution output" controller="execution" style="display:block;padding:5px" action="downloadOutput" id="${execution.id}"><img src="${resource(dir:'images',file:'icon-small-file.png')}" alt="Download" title="Download output" width="13px" height="16px"/></g:link>
                                    </td>
                                    <td style="padding:0px" class="sepL outputlink hilite action  ${execution.dateCompleted && execution?.status!='true'?'fail':''} ${!execution.dateCompleted?'nowrunning':''}"  >
                                        <g:link title="View execution output" controller="execution" style="display:block;padding:5px" action="show" id="${execution.id}">output &raquo;</g:link>
                                    </td>
                                </g:if>
                            </tr>
                            <% j++ %>
                        
                    </g:each>
                </table>
            </g:if>
            <g:else>
                <span class="note empty">None</span>
            </g:else>

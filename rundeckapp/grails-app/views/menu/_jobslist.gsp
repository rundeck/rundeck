
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
                    <g:each in="${jobslist}" var="scheduledExecution">
                        <g:timerStart key="jobslistHead"/>
                        <g:set var="execCount" value="${scheduledExecution.id?Execution.countByScheduledExecution(scheduledExecution):0}"/>
                        <g:set var="nextExecution"
                               value="${ (nextExecutions)? nextExecutions[scheduledExecution.id] : null}"/>
                        <g:set var="currentTime" value="${new Date()}"/>
                        <g:if test="${jobsjscallback}">
                            <tr class=" expandComponentHolder expanded" id="jobrow_${scheduledExecution.id}">
                               <td class="jobname">
                                   <div style="overflow:hidden; text-overflow: ellipsis; height:16px;">
                                       <span class="action textbtn" title="Choose this job" onclick="${jobsjscallback}('${scheduledExecution.jobName}','${scheduledExecution.groupPath}');">${scheduledExecution.jobName.encodeAsHTML()}</span>

                                       <span class="jobdesc" title="${scheduledExecution.description.encodeAsHTML()}">${scheduledExecution.description.encodeAsHTML()}</span>
                                   </div>
                               </td>
                            </tr>
                        </g:if>
                        <g:else>
                        <tr class="sectionhead expandComponentHolder ${paginateParams?.idlist==scheduledExecution.id.toString()?'expanded':''}" id="jobrow_${scheduledExecution.id}">





                            <td class="jobname">
                                <div style="overflow:hidden; text-overflow: ellipsis; height:16px;">
                                <g:expander key="${ukey+'jobDisplay'+scheduledExecution.id}" open="${paginateParams?.idlist==scheduledExecution.id.toString()?'true':'false'}" imgfirst="true">${scheduledExecution.jobName.encodeAsHTML()}</g:expander>

                                <g:if test="${!session.project}">
                                <span class="project">
                                    &bull; <span class="action textbtn" onclick="selectProject('${scheduledExecution.project.encodeAsJavaScript()}');" title="Select this project">${scheduledExecution.project}</span> 
                                </span>
                                </g:if>
                                <span class="jobdesc" title="${scheduledExecution.description.encodeAsHTML()}">${scheduledExecution.description.encodeAsHTML()}</span>
                                <span class="info note ${!execCount?'none':''}" style="margin-left:10px;">
                                    <g:link controller="reports" action="index" params="${[jobIdFilter:scheduledExecution.id]}" title="View all Executions of this job">Executions (${execCount})</g:link>
                                </span>
                                </div>
                            </td>

                        <td class="jobrunning right" colspan="2">
                            <g:if test="${nowrunning && nowrunning[scheduledExecution.id.toString()]}">
                                <g:link class="timenow"
                                    controller="execution"
                                    action="show"
                                    id="${nowrunning[scheduledExecution.id.toString()]}">
                                        <img src="${resource(dir:'images',file:'icon-tiny-disclosure-waiting.gif')}" alt=""/>
                                        now
                                </g:link>
                            </g:if>
                            <g:elseif test="${nextExecution}">
                                    <img src="${resource(dir:'images',file:'icon-clock-small.png')}" alt=""  width="16px" height="16px"/>
                                    <span title="<g:relativeDate atDate='${nextExecution}'/>">
                                    <g:relativeDate elapsed="${nextExecution}" untilClass="timeuntil"/>
                                    </span>
                            </g:elseif>
                            <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
                                    <img src="${resource(dir:'images',file:'icon-clock-small.png')}" alt=""  width="16px" height="16px"/>
                                    <span class="warn note" title="Job schedule will never fire">Never</span>
                            </g:elseif>
                            <g:else>&nbsp;
                            </g:else>
                        </td>

                        </tr>
                            <g:timerEnd key="jobslistHead"/>
                            <g:timerStart key="jobslistBody"/>
                            <g:timerStart key="detail"/>
                        <tr id="${ukey}jobDisplay${scheduledExecution.id}" style="${wdgt.styleVisible('if':paginateParams?.idlist==scheduledExecution.id.toString())}" class=" subsection">
                            <td colspan="3" class="extended jobDetail ">

                                <!--Display job details-->
                                <div class="left">
                                <div class="right">
                                <table width="100%" cellpadding="0" cellspacing="0">
                                    <tr>
                                        <td>
                                <g:render template="/scheduledExecution/showDetail" model="[scheduledExecution:scheduledExecution]"/>


                                        </td>
                                        <td style="width: 80px; vertical-align: top; white-space:nowrap;height:100%; text-align:right" class="jobbuttons ">
                                            <div class="right">
                                            <g:timerEnd key="detail"/>
                                            <g:timerStart key="actions"/>
                                            <g:render template="/scheduledExecution/actionButtons" model="${[scheduledExecution:scheduledExecution,authMap:authMap,jobauthorizations:jobauthorizations]}"/>
                                            <g:timerEnd key="actions"/>
                                            <g:timerStart key="stats"/>
                                            <g:set var="lastrun" value="${scheduledExecution.id?Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution,[max: 1, sort:'dateStarted', order:'desc']):null}"/>
                                            <g:set var="successcount" value="${scheduledExecution.id?Execution.countByScheduledExecutionAndStatus(scheduledExecution,'true'):0}"/>
                                            <g:set var="successrate" value="${execCount>0? (successcount/execCount) : 0}"/>
                                            <g:render template="/scheduledExecution/showStats" model="[scheduledExecution:scheduledExecution,lastrun:lastrun?lastrun:null, successrate:successrate]"/>
                                            <g:timerEnd key="stats"/>
                                            </div>
                                        </td>
                                    </tr>
                                </table>
                                </div>
                                </div>
                                <g:timerEnd key="jobslistBody"/>
                            </td>
                        </tr>
                        </g:else>
                        <% j++ %>
                    </g:each>
                </table>
            </g:if>
                <g:else>
                <span class="note empty">None</span>
            </g:else>
                <g:if test="${total && max && total.toInteger() > max.toInteger() && max.toInteger() > 0}">
                    <span class="info note">Showing ${jobslist.size()} of ${total}</span>
                </g:if>
            %{--<span class="paginate"><g:paginate total="${total}" action="list" max="${max}" params="${paginateParams}"/></span>--}%
        </div>
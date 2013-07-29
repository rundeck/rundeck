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
                        <g:if test="${jobsjscallback}">
                            <tr class=" expandComponentHolder expanded" id="jobrow_${scheduledExecution.id}">
                               <td class="jobname">
                                   <div style="overflow:hidden; text-overflow: ellipsis; height:16px;">
                                       <span class="action textbtn" title="Choose this job" onclick="${jobsjscallback}('${scheduledExecution.jobName}','${scheduledExecution.groupPath}');">${scheduledExecution.jobName.encodeAsHTML()}</span>

                                       <span class="jobdesc" title="${scheduledExecution.description?.encodeAsHTML()}">${scheduledExecution.description?.encodeAsHTML()}</span>
                                   </div>
                               </td>
                            </tr>
                        </g:if>
                        <g:else>
                        <tr class="sectionhead expandComponentHolder ${paginateParams?.idlist==scheduledExecution.id.toString()?'expanded':''}" id="jobrow_${scheduledExecution.id}">
                            <td class="jobname">
                                <div style="overflow:hidden; text-overflow: ellipsis; height: 21px ">
                                    <span class="jobbulkeditfield" style="display: none">
                                    <g:if test="${jobauthorizations && jobauthorizations[AuthConstants.ACTION_DELETE]?.contains(scheduledExecution.id.toString())}">
                                        <input type="checkbox" name="ids" value="${scheduledExecution.extid}"/>
                                    </g:if>
                                    <g:else>
                                        <span class="info note" style="width:12px;margin: 3px;"
                                              title="${message(code: 'unauthorized.job.delete',default: 'Not authorized to delete this job')}"><img
                                                src="${resource(dir: 'images', file: 'icon-tiny-warn.png')}" alt=""
                                                width="12px" height="12px"/></span>
                                    </g:else>
                                </span>
                                    <span class="inlinebuttons jobbuttons">
                                        <div class="buttons">
                                        <g:render template="/scheduledExecution/actionButtons"
                                          model="${[scheduledExecution: scheduledExecution, authMap: authMap, jobauthorizations: jobauthorizations, small: true]}"/>
                                        </div>
                                    </span>

                                    <g:link action="show" controller="scheduledExecution" id="${scheduledExecution.extid}" class="primary" >
                                    ${scheduledExecution.jobName.encodeAsHTML()}</g:link>

                                <g:if test="${jobauthorizations && jobauthorizations[AuthConstants.ACTION_UPDATE]?.contains(scheduledExecution.id.toString())}">
                                    <g:link action="edit" controller="scheduledExecution"
                                            id="${scheduledExecution.extid}"
                                            class="jobIdLink action textbtn">
                                        edit</g:link>
                                </g:if>

                                <span class="jobdesc" title="${scheduledExecution.description?.encodeAsHTML()}">${scheduledExecution.description?.encodeAsHTML()}</span>

                                </div>
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
        </div>

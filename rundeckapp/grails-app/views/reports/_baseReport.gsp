<%@ page import="rundeck.Execution; rundeck.ScheduledExecution; rundeck.ExecReport" %>
<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 7, 2008
  Time: 10:32:26 AM
  To change this template use File | Settings | File Templates.
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:if test="${!options}">
    <g:set var="options" value="[title:true,message:true,end:true,job:true,author:true,stat:true,out:true,context:true,duration:true,cmdinf:true,node:true,msgsplitsize:60]"/>
</g:if>
<%
    if(!options.msgsplitsize){
        options.msgsplitsize=100
    }
    def j = 0;
%>
<g:set var="maxmsgsize" value="${options.evtmaxsize?options.evtmaxsize:options.msgsplitsize?options.msgsplitsize:-1}"/>
<g:set var="maxtitlesize" value="${30}"/>
<table cellpadding="0" cellspacing="0" class="jobsList list history" style="width:100%">
<g:if test="${options.summary}">
    <thead>

    <tr>
        <g:if test="${options.summary}">
        <th colspan="3"><g:message code="execution" /></th>
        %{--<th colspan="1"><g:message code="events.history.title.Summary"/></th>--}%
        </g:if>
        <g:else>
            <th colspan="3"></th>
        </g:else>
        <th><g:message code="jobquery.title.endFilter"/></th>

        %{--<th><g:message code="jobquery.title.userFilter"/></th>--}%
        %{--<th><g:message code="jobquery.title.projFilter"/></th>--}%
        <th colspan="2"><g:message code="events.history.title.Nodes"/></th>
        %{--<th ><g:message code="jobquery.title.duration"/></th>--}%
        <th></th>
    </tr>
    </thead>
    </g:if>
    <g:set var="sincetime" value="${0}"/>
    <g:if test="${hiliteSince}">
        <g:set var="sincetime" value="${hiliteSince instanceof String? Long.parseLong(hiliteSince) : hiliteSince}"/>
    </g:if>
    <g:each in="${reports}">
        <g:set var="rpt" value="${it}"/>
        <g:set var="foundJob" value="${null}"/>
        <g:set var="execution" value="${null}"/>
        <g:set var="status" value="${it?.status=='succeed'?'succeeded':'failed'}"/>
        <g:if test="${rpt?.jcJobId}">
            <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jcJobId)}"/>
        </g:if>
        <g:if test="${rpt?.jcExecId}">
            <g:set var="execution" value="${Execution.get(it.jcExecId)}"/>
            <g:set var="status" value="${execution.status == 'true' ? 'succeeded' : null == execution.dateCompleted ? 'running' : execution.cancelled ? 'killed' : 'failed'}"/>
        </g:if>
        <tr class="  ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} ${sincetime && it.dateCompleted.time>sincetime?'newitem':''} hilite expandComponentHolder sectionhead link" onclick="$(this).down('a._defaultAction').click();">
            <td style="width:12px;">
            %{--<img--}%
                    %{--src="${resource(dir: 'images', file: 'icon-tiny-' + (it?.status == 'succeed' ? 'ok' : 'warn') + '.png')}"--}%
                    %{--alt="" width="12px" height="12px"/>--}%
                <g:if test="${execution}">
                    <g:set var="fileName"
                           value="${execution.status == 'true' ? 'job-ok' : null == execution.dateCompleted ? 'job-running' : execution.cancelled ? 'job-warn' : 'job-error'}"/>
                </g:if>
                <img src="${resource(dir: 'images', file: "icon-small-" + fileName + ".png")}" alt="job" style="border:0;"
                     width="12px" height="12px"/>
            </td>
            <g:set var="vals" value="${['?','?','?']}"/>
            <g:if test="${it instanceof ExecReport}">
                <g:if test="${it?.node=~/^\d+\/\d+\/\d+$/}">
                    <g:set var="vals" value="${it.node.split('/') as List}"/>
                </g:if>
                <g:else>
                    <g:set var="vals" value="${[it?.status=='succeed'?'1':'0',it?.status=='succeed'?'0':'1','1']}"/>
                </g:else>
            </g:if>


        <td class="eventtitle ${rpt?.jcJobId ? 'job' : 'adhoc'}">
            #${rpt.jcExecId}
            <g:if test="${options.summary}">
            <span>
                <g:if test="${rpt?.jcJobId}">
                    <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jcJobId)}"/>
                    <g:if test="${foundJob}">
                        <g:link controller="scheduledExecution" action="show" id="${foundJob.extid}" params="${[fullName:foundJob.generateFullName()]}">
                            ${foundJob.generateFullName().encodeAsHTML()}
                        </g:link>
                    </g:if>
                    <g:else>
                        <span class="warning note">(<g:message
                                code="domain.ScheduledExecution.title"/> ID ${it.jcJobId} has been deleted)</span>
                    </g:else>

                </g:if>
                %{--<g:else>--}%
                    %{--<g:truncate max="${maxtitlesize}" front="true">${rpt?.reportId.encodeAsHTML()}</g:truncate>--}%
                %{--</g:else>--}%
                %{--<g:elseif test="${it.jcJobId }">--}%
                    %{--<g:set var="jobname" value="${job?.generateFullName()}"/>--}%
                    %{--<g:truncate max="${maxtitlesize}" front="true">${jobname.encodeAsHTML()}</g:truncate>--}%
                %{--</g:elseif>--}%
                <g:else>
                    <g:if test="${it instanceof ExecReport && it.adhocScript}">
                        <g:truncate max="${maxmsgsize}">${rpt.adhocScript.encodeAsHTML()}</g:truncate>
                    </g:if>
                    <g:else>
                        <g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>
                    </g:else>
                </g:else>
                </span>
            %{--</td>--}%

            %{--<td style="" class="eventsummary ${rpt?.jcJobId?'job':'adhoc'}">--}%

                %{--<span class="actiontitle ${it?.status != 'succeed' ? '' : ''} ">--}%
                    %{--<g:if test="${it.jcJobId || it.jcExecId}">--}%
                        %{--<g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>--}%
                    %{--</g:if>--}%
                    %{--<g:elseif test="${it instanceof ExecReport && it.adhocScript}">--}%
                        %{--<g:truncate max="${maxmsgsize}">${rpt.adhocScript.encodeAsHTML()}</g:truncate>--}%
                    %{--</g:elseif>--}%
                    %{--<g:else>--}%
                        %{--<g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>--}%
                    %{--</g:else>--}%
                %{--</span>--}%
            %{--</td>--}%
            </g:if>
            <g:else>
                    <g:message code="status.label.${it.status}"/>

                <g:if test="${(status == 'killed')}">
                    by ${it.abortedByUser}
                </g:if>
            </g:else>
        </td>
        <td>
            <g:if test="${execution && execution.argString}"><span class="argString">${execution.argString.encodeAsHTML()}</span></g:if>
        </td>

            <td style="white-space:nowrap" class="right sepL">
                <g:if test="${it.dateCompleted}">
                    <g:relativeDate elapsed="${it?.dateCompleted}" agoClass="timeago"/>
                    <span class="timeabs"><g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/></span>
                    <span title="<g:relativeDate atDate='${it?.dateStarted}'/> to <g:relativeDate
                            atDate='${it?.dateCompleted}'/> ">
                        (<g:relativeDate end="${it?.dateCompleted}" start="${it?.dateStarted}"/>)
                    </span>
                </g:if>
            %{--</td>--}%

            %{--<td class=" sepL user">--}%
                <em>by</em>
                <g:username user="${it?.author}"/>
            </td>

                %{--<td class="project">--}%
                    %{--${it?.ctxProject.encodeAsHTML()}--}%
                %{--</td>--}%

            %{--<td style="white-space:nowrap" class="right sepL">--}%
                    %{--Summary result--}%
                %{--<g:if test="${it.jcJobId || it.jcExecId}">--}%
                    %{--<span class="title"><g:message code="domain.ScheduledExecution.title"/>--}%
                        %{--<g:message code="status.label.${it.status}"/>--}%
                    %{--</span>--}%
                    %{--<g:if test="${(it.status == 'cancel')}">--}%
                        %{--by: ${it.abortedByUser}--}%
                    %{--</g:if>--}%
                %{--</g:if>--}%
            %{--</td>--}%

            <td style="white-space:nowrap;text-align:right;" class="${vals[1] != '0' ? 'fail' : 'ok'}  nodecount sepL">
                <g:if test="${vals[1] != '0'}">
                    ${vals[1]} ${options.summary ? '' : 'node'} failed
                </g:if>
                <g:else>
                    ${vals[0]} ${options.summary ? '' : 'node'} ok
                </g:else>
            </td>

            <td>
                <g:if test="${it instanceof ExecReport && vals}">
                    <g:set var="summary" value=""/>
                    <g:if test="${vals.size() > 2 && vals[2] != '0'}">
                        <g:set var="a" value="${Integer.parseInt(vals[0])}"/>
                        <g:set var="den" value="${Integer.parseInt(vals[2])}"/>
                        <g:set var="fai" value="${Integer.parseInt(vals[1])}"/>
                        <g:set var="sucperc" value="${(int) Math.floor((a / den) * 100)}"/>
                        <g:set var="perc" value="${(int) Math.floor((fai / den) * 100)}"/>
                        <g:if test="${vals[0] && vals[2]}">
                            <g:set var="sucsummary" value="${vals[0] + ' of ' + vals[2]}"/>
                            <g:set var="summary" value="${vals[1] + ' of ' + vals[2]}"/>
                        </g:if>
                    </g:if>
                    <g:else>
                        <g:set var="perc" value="${0}"/>
                    </g:else>
                    <g:if test="${perc > 0}">
                        <g:render template="/common/progressBar"
                                  model="${[completePercent: (int) perc, title: 'Completed nodes', className: 'nodes failure', showpercent: false, innerContent: summary]}"/>
                    </g:if>
                </g:if>
            </td>
            <td style="white-space:nowrap; text-align: right; width:30px;" class="right sepL">
            <g:if test="${rpt.jcExecId}">
                <div class="rptitem">
                    <g:link controller="execution" action="show" id="${rpt.jcExecId}" class="_defaultAction"
                            title="View execution output">Show &raquo;</g:link>
                </div>
            </g:if>
            </td>
        </tr>
        %{--<g:render template="expandedReportContent" model="[it:it,colspan:9,subkey:rkey+'subsect',index:j]"/>--}%
        <% j++; %>
    </g:each>
</table>

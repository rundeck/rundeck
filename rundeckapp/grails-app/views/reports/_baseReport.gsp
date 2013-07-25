<%@ page import="rundeck.Execution; rundeck.ScheduledExecution; rundeck.ExecReport" %>
<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 7, 2008
  Time: 10:32:26 AM
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
            <td style="width:12px;" class="eventicon">
                <g:if test="${execution}">
                    <g:set var="fileName"
                           value="${execution.status == 'true' ? 'job-ok' : null == execution.dateCompleted ? 'job-running' : execution.cancelled ? 'job-warn' : 'job-error'}"/>
                </g:if>
                <img src="${resource(dir: 'images', file: "icon-small-" + fileName + ".png",absolute: absoluteLinks)}" alt="job" style="border:0;"
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
                <g:if test="${rpt?.jcJobId}">
                    <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jcJobId)}"/>
                    <g:if test="${foundJob}">
                        ${foundJob.groupPath?foundJob.groupPath+'/':''}${foundJob.jobName.encodeAsHTML()}
                    </g:if>
                    <g:else>
                        <span class="warning note">(<g:message
                                code="domain.ScheduledExecution.title"/> ID ${it.jcJobId} has been deleted)</span>
                    </g:else>

                </g:if>
                <g:else>
                    <g:truncate max="${maxmsgsize}">${rpt.title.encodeAsHTML()}</g:truncate>
                </g:else>
            </g:if>
            <g:else>
                    <g:message code="status.label.${it.status}"/>

                <g:if test="${(status == 'killed')}">
                    by ${it.abortedByUser}
                </g:if>
            </g:else>
        </td>
        <td class="eventargs">
            <g:if test="${execution && execution.argString}"><span class="">${execution.argString.encodeAsHTML()}</span></g:if>
            <g:if test="${params.debug}">
                ${rpt.toMap()}
            </g:if>
        </td>

            <td style="white-space:nowrap" class="right sepL">
                <g:if test="${it.dateCompleted}">
                    %{--<g:relativeDate elapsed="${it?.dateCompleted}" agoClass="timeago"/>--}%
                    <g:unless test="${hideDate}">
                    <span class="timeabs"><g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/></span>
                    </g:unless>
                    <span title="<g:relativeDate atDate='${it?.dateStarted}'/> to <g:relativeDate
                            atDate='${it?.dateCompleted}'/> ">
                        in <g:relativeDate end="${it?.dateCompleted}" start="${it?.dateStarted}"/>
                    </span>
                </g:if>
            </td>

            <td class="  user" style="white-space: nowrap">
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
                        %{--<g:render template="/common/progressBar"--}%
                                  %{--model="${[completePercent: (int) perc, title: 'Completed nodes', className: 'nodes failure', showpercent: false, innerContent: summary]}"/>--}%
                    </g:if>
                </g:if>
            </td>
            <g:unless test="${hideShowLink}">
            <td style="white-space:nowrap; text-align: right; width:30px;" class="sepL">
            <g:if test="${rpt.jcExecId}">
                <div class="rptitem">
                    <g:link controller="execution" action="show" id="${rpt.jcExecId}" class="_defaultAction"
                            title="View execution output" absolute="${absoluteLinks}">Show &raquo;</g:link>
                </div>
            </g:if>
            </td>
            </g:unless>
        </tr>
        %{--<g:render template="expandedReportContent" model="[it:it,colspan:9,subkey:rkey+'subsect',index:j]"/>--}%
        <% j++; %>
    </g:each>
</table>

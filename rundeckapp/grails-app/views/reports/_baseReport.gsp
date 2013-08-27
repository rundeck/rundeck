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
    <g:set var="sincetime" value="${0}"/>
    <g:if test="${hiliteSince}">
        <g:set var="sincetime" value="${hiliteSince instanceof String? Long.parseLong(hiliteSince) : hiliteSince}"/>
    </g:if>
    <g:each in="${reports}">
        <g:set var="rpt" value="${it}"/>
        <g:set var="foundJob" value="${null}"/>
        <g:set var="execution" value="${null}"/>
        <g:set var="status" value="${it?.status=='succeed'?'succeeded':'failed'}"/>
        <g:if test="${rpt?.jobId}">
            <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jobId)}"/>
        </g:if>
        <g:if test="${rpt?.execId}">
            <g:set var="execution" value="${Execution.get(it.execId)}"/>
            <g:set var="status" value="${execution.status == 'true' ? 'succeeded' : null == execution.dateCompleted ? 'running' : execution.cancelled ? 'killed' : 'failed'}"/>
        </g:if>
        <tr class="link  ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} ${sincetime && it.dateCompleted.time>sincetime?'newitem':''} hilite " onclick="$(this).down('a._defaultAction').click();">
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


        <td class="eventtitle ${rpt?.jobId ? 'job' : 'adhoc'}" colspan="${rpt?.jobId?1:2}">
            #${rpt.execId}
            <g:if test="${options.summary}">
                <g:if test="${rpt?.jobId}">
                    <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jobId)}"/>
                    <g:if test="${foundJob}">
                        ${foundJob.groupPath?foundJob.groupPath+'/':''}${foundJob.jobName.encodeAsHTML()}
                    </g:if>
                    <g:else>
                        <span class="warning note">(<g:message
                                code="domain.ScheduledExecution.title"/> ID ${it.jobId} has been deleted)</span>
                    </g:else>

                </g:if>
                <g:else>
                    ${rpt.title.encodeAsHTML()}
                </g:else>
            </g:if>
            <g:else>
                    <g:message code="status.label.${it.status}"/>

                <g:if test="${(status == 'killed')}">
                    by ${it.abortedByUser}
                </g:if>
            </g:else>
        </td>
            <g:if test="${rpt?.jobId}">
        <td class="eventargs">
            <g:if test="${execution && execution.argString}"><span class="">${execution.argString.encodeAsHTML()}</span></g:if>
            <g:if test="${params.debug}">
                ${rpt.toMap()}
            </g:if>
        </td>
            </g:if>

            <td style="white-space:nowrap" class="right sepL date">
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

            <td style="white-space:nowrap;text-align:right;" class="${vals[1] != '0' ? 'fail' : 'ok'}  nodecount sepL">
                <g:if test="${vals[1] != '0'}">
                    ${vals[1]} ${options.summary ? '' : 'node'} failed
                </g:if>
                <g:else>
                    ${vals[0]} ${options.summary ? '' : 'node'} ok
                </g:else>
            </td>

            <g:unless test="${hideShowLink}">
            <td class="sepL outputlink">
            <g:if test="${rpt.execId}">
                <g:link controller="execution" action="show" id="${rpt.execId}" class="_defaultAction"
                        title="View execution output" absolute="${absoluteLinks}">Show &raquo;</g:link>
            </g:if>
            </td>
            </g:unless>
        </tr>
        <% j++; %>
    </g:each>
<g:if test="${lastDate}">
    <g:set var="checkUpdatedParams" value="${[since: lastDate]}"/>
    %{
        if (filterName) {
            checkUpdatedParams.filterName = filterName
        } else {
            checkUpdatedParams.putAll(paginateParams)
        }
    }%
    <g:set var="checkUpdatedUrl" value="${g.createLink(action: 'since.json', params: checkUpdatedParams)}"/>
</g:if>
<g:set var="refreshUrl"
       value="${g.createLink(action: 'eventsFragment', params: filterName ? [filterName: filterName] : paginateParams)}"/>
<g:set var="rssUrl"
       value="${g.createLink(controller: 'feed', action: 'index', params: filterName ? [filterName: filterName] : paginateParams)}"/>
<g:render template="/common/boxinfo"
          model="${[name: 'events', model: [total: total, max: max, offset: offset, url: refreshUrl, checkUpdatedUrl: checkUpdatedUrl, rssUrl: rssUrl, lastDate: lastDate]]}"/>

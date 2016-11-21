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
        <g:if test="${rpt?.jcJobId}">
            <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jcJobId)}"/>
        </g:if>
        <g:if test="${rpt?.jcExecId}">
            <g:set var="execution" value="${Execution.get(it.jcExecId)}"/>
            <g:set var="status" value="${execution.executionState}"/>
        </g:if>
        <tr class="link autoclick ${it?.status != 'succeed' ? 'fail' : ''}  ${!it.dateCompleted ? 'nowrunning' : ''} ${sincetime && it.dateCompleted.time>sincetime?'newitem':''}  " >
            <g:if test="${!hideEdit}">
            <td style="display: none" class="eventicon obs_bulk_edit_enable">
                <input type="checkbox" value="${enc(attr:rpt.jcExecId)}" name="bulk_edit" class="_defaultInput bulk_edit"/>
            </td>
            </g:if>
            <g:set var="statusIcon" value="${execution.status == 'scheduled' ? 'time' : !execution.dateCompleted ? 'running' : execution.statusSucceeded() ?
                    'succeed' : execution.cancelled ? 'aborted' :execution.willRetry ? 'failedretry' :execution.timedOut ? 'timedout' :
                    execution.status in ['false','failed']?'fail':'other'}"/>
            <g:set var="statusIcon" value="${[succeeded:'succeed','failed-with-retry':'failedretry',failed:'fail'].get(status)?:status}"/>
            <td class="eventicon autoclickable">
                <i class="exec-status icon ${statusIcon}"></i>
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


        <td class="eventtitle ${rpt?.jcJobId ? 'job' : 'adhoc'} autoclickable" colspan="${rpt?.jcJobId?1:2}">
            <g:link controller="execution" action="show" id="${rpt.jcExecId}" class="_defaultAction"
                params="[project:execution?execution.project:rpt.ctxProject?:params.project]"
                    title="View execution output" absolute="${absoluteLinks}">#<g:enc>${rpt.jcExecId}</g:enc></g:link>
            <g:if test="${options.summary}">
                <g:if test="${rpt?.jcJobId}">
                    <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(it.jcJobId)}"/>
                    <g:if test="${foundJob}">
                        <g:enc>${foundJob.groupPath ? foundJob.groupPath+'/':''}${foundJob.jobName}</g:enc>
                    </g:if>
                    <g:else>
                        <span class="text-muted">(<g:message
                                code="domain.ScheduledExecution.title"/> ID <g:enc>${it.jcJobId}</g:enc> has been deleted)</span>
                    </g:else>

                </g:if>
                <g:else>
                    <g:enc>${rpt.title}</g:enc>
                </g:else>
            </g:if>
            <g:else>
                <g:if test="${!it.status}">
                    <g:message code="status.label.${it.status}"/>
                </g:if>
                <g:if test="${(status == 'killed')}">
                    by <g:enc>${it.abortedByUser}</g:enc>
                </g:if>
            </g:else>
            <g:if test="${statusIcon=='other'}">
                <span class="exec-status-text custom-status">${execution.status}</span>
            </g:if>
        </td>
            <g:if test="${rpt?.jcJobId}">
        <td class="eventargs autoclickable">
            <div class="argstring-scrollable">
            <g:if test="${execution && execution.argString}">
                <g:render template="/execution/execArgString" model="[argString: execution.argString]"/>
            </g:if>
            <g:if test="${params.debug}">
                <g:enc>${rpt.toMap()}</g:enc>
            </g:if>
            </div>
        </td>
            </g:if>

            <td style="white-space:nowrap" class="right  date autoclickable">
                <g:if test="${it.dateCompleted}">
                    %{--<g:relativeDate elapsed="${it?.dateCompleted}" agoClass="timeago"/>--}%
                    <g:unless test="${hideDate}">
                    <span class="timeabs"><g:formatDate date="${it?.dateCompleted}" formatName="jobslist.date.format"/></span>
                    </g:unless>
                    <g:if test="${it?.dateStarted?.getTime() < it?.dateCompleted?.getTime()}">
                        <span title="<g:relativeDate atDate='${it?.dateStarted}'/> to <g:relativeDate
                                atDate='${it?.dateCompleted}'/> ">
                            in <g:relativeDate end="${it?.dateCompleted}" start="${it?.dateStarted}"/>
                        </span>
                    </g:if>
                </g:if>
            </td>

            <td class="  user autoclickable" style="white-space: nowrap">
                <em>by</em>
                <g:username user="${it?.author}"/>
            </td>


            <td class="  user autoclickable" style="white-space: nowrap">
                <g:if test="${it?.filterApplied}">
                    <em><g:message code="activity.jobs.executed.node"/>:</em>
                    ${it?.filterApplied}
                </g:if>
                <g:else>
                    <em><g:message code="activity.jobs.executed.local"/></em>
                </g:else>
            </td>

            <g:unless test="${hideNodes}">
            <td class="${vals[1] != '0' ? 'fail' : 'ok'}  nodecount autoclickable ">
                <g:if test="${vals[1] != '0'}">
                    <g:enc>${vals[1]}</g:enc> ${options.summary ? '' : 'node'} failed
                </g:if>
                <g:else>
                    <g:enc>${vals[0]}</g:enc> ${options.summary ? '' : 'node'} ok
                </g:else>
            </td>
            </g:unless>

        </tr>
        <% j++; %>
    </g:each>
<g:if test="${lastDate}">
    <g:set var="checkUpdatedParams" value="${[since: lastDate,project:params.project]}"/>
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

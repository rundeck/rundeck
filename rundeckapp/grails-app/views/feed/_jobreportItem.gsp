
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
<g:set var="rkey" value="${g.rkey()}"/>
<g:set var="options" value="[summary:true]"/>
<%
    if(!options.msgsplitsize){
        options.msgsplitsize=100
    }
    def j = 0;
%>
<g:set var="rpt" value="${report}"/>
<g:set var="foundJob" value="${null}"/>
<g:set var="execution" value="${null}"/>
<g:set var="status" value="${rpt?.status=='succeed'?'succeeded':'failed'}"/>
<g:if test="${rpt?.jcJobId}">
    <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(rpt.jcJobId)}"/>
</g:if>
<g:if test="${rpt?.executionId}">
    <g:set var="execution" value="${Execution.get(rpt.executionId)}"/>
    <g:set var="status" value="${execution.executionState}"/>
</g:if>
<div class="" >
    <g:set var="statusIcon" value="${!execution.dateCompleted ? 'running' : execution.statusSucceeded() ?
            'succeed' : execution.cancelled ? 'aborted' :execution.willRetry ? 'failedretry' :execution.timedOut ? 'timedout' :
            execution.status in ['false','failed']?'fail':'other'}"/>
    <g:set var="statusIcon" value="${[succeeded:'succeed','failed-with-retry':'failedretry',failed:'fail'].get(status)?:status}"/>
    <span>
        <i class="exec-status icon ${statusIcon}"></i>
    </span>
    <g:set var="vals" value="${['?','?','?']}"/>
    <g:if test="${rpt instanceof ExecReport}">
        <g:if test="${rpt?.node=~/^\d+\/\d+\/\d+$/}">
            <g:set var="vals" value="${rpt.node.split('/') as List}"/>
        </g:if>
        <g:else>
            <g:set var="vals" value="${[rpt?.status=='succeed'?'1':'0',rpt?.status=='succeed'?'0':'1','1']}"/>
        </g:else>
    </g:if>


    <span >
        <g:link controller="execution" action="show" id="${rpt.executionId}" class="_defaultAction"
                params="[project:execution?execution.project:rpt.ctxProject?:params.project]"
                title="View execution output" absolute="true">#<g:enc>${rpt.executionId}</g:enc></g:link>
        <g:if test="${rpt?.jcJobId}">
            <g:set var="foundJob" value="${ScheduledExecution.getByIdOrUUID(rpt.jcJobId)}"/>
            <g:if test="${foundJob}">
                &raquo;${foundJob.groupPath ? foundJob.groupPath+'/':''}${foundJob.jobName}&laquo;
            </g:if>
            <g:else>
                <span class="text-strong">(<g:message
                        code="domain.ScheduledExecution.title"/> ID <g:enc>${rpt.jcJobId}</g:enc> has been deleted)</span>
            </g:else>

        </g:if>
        <g:else>
            <g:enc>${rpt.title}</g:enc>
        </g:else>

        <g:if test="${statusIcon=='other'}">
            <span class="exec-status-text custom-status">${execution.status}</span>
        </g:if>
    </span>
    <g:if test="${rpt?.jcJobId}">
        <span >
            <g:if test="${execution && execution.argString}">
                ${execution.argString}
                %{--<g:render template="/execution/execArgString" model="[argString: execution.argString]"/>--}%
            </g:if>
            <g:if test="${params.debug}">
                <g:enc>${rpt.toMap()}</g:enc>
            </g:if>
        </span>
    </g:if>

    [${status}]

    <span style="white-space:nowrap" >
        <g:if test="${rpt.dateCompleted}">
        %{--<g:relativeDate elapsed="${rpt?.dateCompleted}" agoClass="timeago"/>--}%
        %{--<g:unless test="${hideDate}">--}%
        %{--<span class="timeabs"><g:formatDate date="${rpt?.dateCompleted}" formatName="jobslist.date.format"/></span>--}%
        %{--</g:unless>--}%
            <span title="<g:relativeDate atDate='${rpt?.dateStarted}'/> to <g:relativeDate
                    atDate='${rpt?.dateCompleted}'/> ">
                <g:message code="in.of" default="in"/> <g:relativeDate end="${rpt?.dateCompleted}" start="${rpt?.dateStarted}"/>
            </span>
        </g:if>
    </span>

    <span style="white-space: nowrap">
        <em><g:message code="by" default="by"/></em>
        <g:username user="${rpt?.author}"/>
    </span>

    <g:unless test="${hideNodes}">
        <span>
            <g:if test="${vals[1] != '0'}">
                <g:enc>${vals[1]}</g:enc> node failed
            </g:if>
            <g:else>
                <g:enc>${vals[0]}</g:enc> node ok
            </g:else>
        </span>
    </g:unless>

</div>
<style>

</style>

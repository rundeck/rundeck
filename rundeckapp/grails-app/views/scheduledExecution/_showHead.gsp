<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>
<g:set var="execInfo" value="${scheduledExecution}"/>

<div class="jobHead col-sm-12" >
<div class="jobInfo " id="jobInfo_">
    <span class="jobInfoSection h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            ${scheduledExecution?.jobName.encodeAsHTML()}
        </g:link>
    </span>
        <g:if test="${scheduledExecution.scheduled && nextExecution}">
            <span class="scheduletime">
                <i class="glyphicon glyphicon-time"></i>
                <g:set var="titleHint"
                       value="${remoteClusterNodeUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''}"/>
                <span title="${clusterUUID ? g.message(code: "expecting.another.cluster.server.to.run") : ''} at ${g.relativeDate(atDate:nextExecution)}">
                    <g:relativeDate elapsed="${nextExecution}"
                                    untilClass="timeuntil"/>
                </span>
            </span>
        </g:if>
        <g:elseif test="${scheduledExecution.scheduled && !nextExecution}">
            <span class="scheduletime willnotrun has_tooltip" data-toggle="tooltip"
                data-placement="auto left"
                  title="${g.message(code: 'job.schedule.will.never.fire')}">
                <i class="glyphicon glyphicon-time"></i>
                <span class="detail"><g:message code="never" /></span>
            </span>
        </g:elseif>
        <span class="h4 jobInfoSection">
            <span class="text-muted">${execInfo?.description?.encodeAsHTML()}</span>
        </span>

        <span class="h4  jobGroup ">
            <g:if test="${execInfo.groupPath}">
                <g:link controller="menu" action="jobs"
                        class="secondary"
                        params="${[groupPath: execInfo.groupPath]}"
                        title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                        absolute="${absolute ? 'true' : 'false'}">
                    <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                    ${execInfo.groupPath.encodeAsHTML()}
                </g:link>
            </g:if>
        </span>
</div>
</div>

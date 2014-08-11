<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>

<div class="jobHead col-sm-12" >
<div class="jobInfo " id="jobInfo_">
    <span class="jobInfoSection h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
            params="[project: scheduledExecution.project]"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            <g:enc>${scheduledExecution?.jobName}</g:enc></g:link>
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

        <span class="h4  jobGroup ">
            <g:if test="${scheduledExecution.groupPath}">
                <g:link controller="menu" action="jobs"
                        class="secondary"
                        params="${[groupPath: scheduledExecution.groupPath, project:scheduledExecution.project]}"
                        title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                        absolute="${absolute ? 'true' : 'false'}">
                    <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                    <g:enc>${scheduledExecution.groupPath}</g:enc>
                </g:link>
            </g:if>
        </span>
    <div class="h4 jobInfoSection">
        <span class="text-muted"><g:enc>${scheduledExecution?.description}</g:enc></span>
    </div>

</div>
</div>

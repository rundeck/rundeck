<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>
<g:set var="execInfo" value="${scheduledExecution}"/>

<div class="jobInfo" id="jobInfo_">
    <div class="jobInfoSection h3">
        <g:link controller="scheduledExecution" action="show"
            class="primary"
                id="${scheduledExecution.extid}"
                absolute="${absolute ? 'true' : 'false'}">
            <i class="glyphicon glyphicon-book"></i>
            ${scheduledExecution?.jobName.encodeAsHTML()}
        </g:link>

        <g:if test="${!runPage}">
            <small>
            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                <g:link controller="scheduledExecution" title="Edit or Delete this Job" action="edit"
                        id="${scheduledExecution.extid}" class="textbtn textbtn-info textbtn-on-hover">
                <b class="glyphicon glyphicon-pencil"></b>
                    edit job</g:link>
            </g:if>
            </small>
        </g:if>

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
    </div>

    <div class="jobInfoSection h4 jobGroup">
        <g:if test="${execInfo.groupPath}">
            <g:link controller="menu" action="jobs" params="${[groupPath: execInfo.groupPath]}"
                class=""
                    title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                    absolute="${absolute ? 'true' : 'false'}">
                <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                ${execInfo.groupPath.encodeAsHTML()}
            </g:link>
        </g:if>
    </div>

    <div class="h4 jobInfoSection">
        <span class="text-muted">${execInfo?.description?.encodeAsHTML()}</span>
    </div>
</div>

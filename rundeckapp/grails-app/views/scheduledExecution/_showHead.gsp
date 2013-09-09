<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>
<g:set var="execInfo" value="${scheduledExecution}"/>
<g:set var="jobPrimary" value="${'primary'}"/>
<g:set var="execPrimary" value="${''}"/>

<div class="jobInfo" id="jobInfo_">
    <div class="jobInfoSection h3">
        <span class="${jobPrimary}">
            <g:link controller="scheduledExecution" action="show"
                    id="${scheduledExecution.extid}"
                    absolute="${absolute ? 'true' : 'false'}">
                <span class="jobName">${scheduledExecution?.jobName.encodeAsHTML()}</span></g:link>

        </span>
        <g:if test="${!runPage}">
            <small>
            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                <g:link controller="scheduledExecution" title="Edit or Delete this Job" action="edit"
                        id="${scheduledExecution.extid}" class="textbtn textbtn-info">
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

    <div class="jobInfoSection">

        <span class="jobInfoPart ${jobPrimary}">
            <g:if test="${execInfo.groupPath}">
                <span class="jobGroup">
                    <span class="grouplabel">
                        <g:link controller="menu" action="jobs" params="${[groupPath: execInfo.groupPath]}"
                                title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                                absolute="${absolute ? 'true' : 'false'}">
                            <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                            ${execInfo.groupPath.encodeAsHTML()}
                        </g:link>
                    </span>
                </span>
            </g:if>

        </span>

    </div>

    <div class="jobInfoSection">
        <span class="text-muted">${execInfo?.description?.encodeAsHTML()}</span>
    </div>
</div>

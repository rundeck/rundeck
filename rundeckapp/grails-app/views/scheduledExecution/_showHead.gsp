<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.ScheduledExecution" %>
<g:set var="execInfo" value="${scheduledExecution}"/>
<g:set var="jobPrimary" value="${'primary'}"/>
<g:set var="execPrimary" value="${''}"/>

<div class="jobInfo" id="jobInfo_">
    <div class="jobInfoSection">
        <span class="jobInfoPart ${jobPrimary}">
            <g:link controller="scheduledExecution" action="show"
                    id="${scheduledExecution.extid}"
                    absolute="${absolute ? 'true' : 'false'}">
                <span class="jobName">${scheduledExecution?.jobName.encodeAsHTML()}</span></g:link>

        </span>
        <g:if test="${!runPage}">
            <span>
            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
                <g:link controller="scheduledExecution" title="Edit or Delete this Job" action="edit"
                        id="${scheduledExecution.extid}" class="textbtn">
                <img
                src="${resource(dir: 'images', file: 'icon-tiny-edit.png')}" alt="edit" width="12px"
                height="12px"/>
                    edit job</g:link>
            </g:if>
            </span>
            <span>

        </g:if>
    </div>

    <div class="jobInfoSection">

        <span class="jobInfoPart ${jobPrimary}">
            <g:if test="${execInfo.groupPath}">
                <span class="jobGroup">
                    <span class="grouplabel">
                        <g:link controller="menu" action="jobs" params="${[groupPath: execInfo.groupPath]}"
                                title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                                absolute="${absolute ? 'true' : 'false'}">
                            <g:if test="${!noimgs}"><img src="${resource(dir: 'images', file: 'icon-small-folder.png')}"
                                                         width="16px" height="15px" alt=""/></g:if>
                            ${execInfo.groupPath.encodeAsHTML()}
                        </g:link>
                    </span>
                </span>
            </g:if>

        </span>

    </div>

    <div class="jobInfoSection">
        <span class="jobdesc">${execInfo?.description?.encodeAsHTML()}</span>
    </div>
</div>

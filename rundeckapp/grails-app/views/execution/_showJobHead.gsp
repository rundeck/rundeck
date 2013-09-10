<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:if test="${scheduledExecution}">
    <div class="jobInfoSection">
        <span class="jobInfoPart secondary">
            <g:if test="${!groupOnly}">
            <g:link controller="scheduledExecution" action="show"
                    id="${scheduledExecution.extid}"
                    class="primary" absolute="${absolute ? 'true' :'false'}"
                title="${scheduledExecution?.description.encodeAsHTML()}"
            >
                ${scheduledExecution?.jobName.encodeAsHTML()}
            </g:link>

            </g:if>
            <g:if test="${scheduledExecution.groupPath && !nameOnly}">
            <span class="jobGroup">
                <span class="grouplabel">
                    <g:link controller="menu" action="jobs"
                            params="${[groupPath: scheduledExecution.groupPath]}"
                            title="${'View ' + g.message(code: 'domain.ScheduledExecution.title') + 's in this group'}"
                            absolute="${absolute ? 'true' : 'false'}">
                        <g:if test="${!noimgs}"><b class="glyphicon glyphicon-folder-close"></b></g:if>
                        ${scheduledExecution.groupPath.encodeAsHTML()}
                    </g:link>
                </span>
            </span>
            </g:if>
        </span>
        <g:if test="${!groupOnly && auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_UPDATE)}">
            <g:link controller="scheduledExecution" title="Edit Job" action="edit" id="${scheduledExecution.extid}"
                class="textbtn textbtn-info">
                <i class="glyphicon glyphicon-pencil"></i>
                edit
            </g:link>
        </g:if>
    </div>
</g:if>
